var module = angular.module(PKG.name+'.controllers');



/**
 * CrudListBase
 * a base to be extended by list controllers
 */
module.factory('CrudListBase', function CrudListBaseFactory() {
  return function CrudListBase () {
    var scope = this;

    // we already fetched the list in the parent view
    scope.$watch('subnavList', function (list) {
      if(list) {
        scope.list = list;
      }
    });

    // but we want it to be fresh
    if(!scope.subnavList || scope.subnavList.$resolved) {
      scope.fetchSubnavList();
    }

    scope.doDelete = function (model) {
      model.$delete(function () {
        scope.fetchSubnavList();
      });
    };

  };
});



/**
 * CrudFormBase
 * a base to be extended by CrudEditCtrl and CrudCreateCtrl
 */
module.factory('CrudFormBase', function CrudFormBaseFactory ($injector) {
  return function CrudFormBase () {
    var $state = $injector.get('$state'),
        myApi = $injector.get('myApi'),
        scope = this;

    scope.doSubmit = function (model) {
      doThenList(model, $state.includes('*.create') ? '$save' : '$update');
    };

    scope.doDelete = function (model) {
      doThenList(model, '$delete');
    };

    /* ----------------------------------------------------------------------- */
  
    function doThenList(model, method) {
      scope.submitting = true;

      if(!angular.isFunction(model[method]) ) {
        // happens using jsonEdit directive directly on model
        model = new myApi[$state.current.data.modelName](model);
      }

      model[method]()
        .then(function () {
          scope.fetchSubnavList();
          $state.go('^.list');
        })
        .finally(function () {
          scope.submitting = false;
        });
    }


  };
});

