#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require_relative 'utils'

class FogProviderJoyent < Provider
  include FogProvider

  # plugin defined resources
  @ssh_key_dir = 'ssh_keys'
  class << self
    attr_accessor :ssh_key_dir
  end

  def create(inputmap)
    flavor = inputmap['flavor']
    image = inputmap['image']
    hostname = inputmap['hostname']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Create the server
      log.debug "Creating #{hostname} on Joyent using flavor: #{flavor}, image: #{image}"
      log.debug 'Invoking server create'
      begin
        server = connection.servers.create(
          package: flavor,
          dataset: image,
          name: hostname,
          key_name: @ssh_keypair
        )
      end
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = @task['config']['sshuser'] || 'root'
      @result['result']['ssh-auth']['identityfile'] = File.join(Dir.pwd, self.class.ssh_key_dir, @ssh_key_resource) unless @ssh_key_resource.nil?
      @result['status'] = 0
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderJoyent.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderJoyent.create: #{e.inspect}"
    else
      log.debug "Create finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def confirm(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Confirm server
      log.debug "Invoking server confirm for id: #{providerid}"
      server = connection.servers.get(providerid)
      # Wait until the server is ready
      fail "Server #{server.name} is in ERROR state" if server.state == 'ERROR'
      log.debug "waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      bootstrap_ip = ip_address(server)
      if bootstrap_ip.nil?
        log.error 'No IP address available for bootstrapping.'
        fail 'No IP address available for bootstrapping.'
      else
        log.debug "Bootstrap IP address #{bootstrap_ip}"
      end

      wait_for_sshd(bootstrap_ip, 22)
      log.debug "Server #{server.name} sshd is up"

      # Process results
      @result['ipaddresses'] = {
        'access_v4' => bootstrap_ip,
        'bind_v4' => bootstrap_ip
      }
      # do we need sudo bash?
      sudo = 'sudo' unless @task['config']['ssh-auth']['user'] == 'root'
      set_credentials(@task['config']['ssh-auth'])
      # Validate connectivity
      Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
        ssh_exec!(ssh, "#{sudo} hostname #{@task['config']['hostname']}", 'Temporarily setting hostname')
        # Check for /dev/vdb
        begin
          vdb = true
          # confirm /dev/vdb exists
          ssh_exec!(ssh, 'test -e /dev/vdb && echo yes', 'Checking for /dev/vdb')
          # confirm it is not already mounted
          #   ubuntu: we remount from /mnt to /data
          #   centos: vdb1 already mounted at /data
          ssh_exec!(ssh, 'if grep "vdb.* /data " /proc/mounts ; then /bin/false ; fi', 'Checking if /dev/vdb mounted already')
        rescue
          vdb = false
        end
        if vdb
          ssh_exec!(ssh, "mount | grep ^/dev/vdb 2>&1 >/dev/null && #{sudo} umount /dev/vdb && #{sudo} /sbin/mkfs.ext4 /dev/vdb && #{sudo} mkdir -p /data && #{sudo} mount -o _netdev /dev/vdb /data", 'Mounting /dev/vdb as /data')
          ssh_exec!(ssh, "#{sudo} sed -i -e 's:/mnt:/data:' /etc/fstab", 'Updating /etc/fstab for /data')
        end
      end
      # Return 0
      @result['status'] = 0
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
      @result['stderr'] = 'Timed out waiting for server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{bootstrap_ip}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{bootstrap_ip}: #{e.inspect}"
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderJoyent.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderJoyent.confirm: #{e.inspect}"
    else
      log.debug "Confirm finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def delete(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Delete server
      log.debug 'Invoking server delete'
      begin
        server = connection.servers.get(providerid)
        server.destroy
      rescue NoMethodError
        log.warn "Could not locate server '#{providerid}'... skipping"
      else
        sleep 30
      end
      # Return 0
      @result['status'] = 0
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderJoyent.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderJoyent.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # Shared definitions (borrowed from knife-joyent gem, Apache 2.0 license)

  def connection
    # Create connection
    # rubocop:disable UselessAssignment
    @connection ||= begin
      connection = Fog::Compute.new(
        provider: 'Joyent',
        joyent_username: @api_user,
        joyent_password: @api_password,
        joyent_keyname: @ssh_keypair,
        joyent_keyfile: File.join(self.class.ssh_key_dir, @ssh_key_resource),
        joyent_url: @joyent_api_url,
        joyent_version: @joyent_version
      )
    end
    # rubocop:enable UselessAssignment
  end

  def ip_address(server)
    server_ips = server.ips.select { |ip| ip && !(loopback?(ip) || linklocal?(ip)) }
    if server_ips.count === 1
      server_ips.first
    else
      server_ips.find { |ip| !private?(ip) }
    end
  end
end
