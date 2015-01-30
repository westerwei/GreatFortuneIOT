/**
 * AngularJS script for IOTApp module 
 */
//var remoteHostIP = "192.168.1.101";
(function(angular) {
	'use strict';
	//define our application and pull in ngRoute and ngAnimate
	var iotApp = angular.module('IOTApp', ['ngRoute', 'ngAnimate']);
  
	//ROUTING ===============================================
	//set our routing for this application
	//each route will pull in a different controller
	iotApp.config(function($routeProvider, $locationProvider) {
	
	   $routeProvider
	
	       // home page
	       .when('/', {
	           templateUrl: '/iotapp/admin/loginSys',
	           controller: 'loginController'
	       })
	
	       // dashboard page
	       .when('/dashboard', {
	           templateUrl: '/iotapp/admin/dashboard',
	           controller: 'dashboardController'
	       })
	
	       // contact page
//	       .when('/contact', {
//	           templateUrl: 'page-contact.html',
//	           controller: 'contactController'
//	       });
	       
	       // use the HTML5 History API
           //$locationProvider.html5Mode(true);
	       //$locationProvider.hashPrefix('!');
	
	});

	iotApp.controller('loginController', ['$scope','$http', '$location', function($scope, $http, $location) {
		  console.log('in loginController......');
		  $scope.pageClass = 'login-page';
		  $scope.instVar = 'Controller Var Test';
	      $scope.resultMsg = '';
	      $scope.resultCode = '';
	      $scope.uidisable = false;
	      $scope.displayMessage = '';
	      $scope.msgStyle = ''; //alert-danger , alert-success 
	      $scope.msgStylePrefix = 'form-group alert ';
	      
	      $scope.recoverUI = function(){
	    	  $scope.uidisable = false;
	    	  $( "#msgBox" ).fadeOut( "slow" );
	      };
	      
	      $scope.Login = function(){
//	    	  window.alert("Test123");
	    	  $scope.uidisable = true;
	    	  
	    	  /*
	    	   * $http.post("/customer/data/autocomplete", {term: searchString}, {headers: {'Content-Type': 'application/json'} })
	        		.then(function (response) {
	            		return response;
	        		});
	    	   */
	    	  /*
	    	   * $http.get('/someUrl').success(successCallback);
				 $http.post('/someUrl', data).success(successCallback);
				 Returns
					HttpPromise	
					Returns a promise object with the standard then method and two http specific methods: success and error. The then method takes two arguments a success and an error callback which will be called with a response object. The success and error methods take a single argument - a function that will be called when the request succeeds or fails respectively. The arguments passed into these functions are destructured representation of the response object passed into the then method. The response object has these properties:
					
					data – {string|Object} – The response body transformed with the transform functions.
					status – {number} – HTTP status code of the response.
					headers – {function([headerName])} – Header getter function.
					config – {Object} – The configuration object that was used to generate the request.
					statusText – {string} – HTTP status text of the response.
	    	   * 
	    	   */
	    	  
	    	  $http(
	    			  {
	    				  method: "POST",
	    				  url: "http://" + remoteHostIP + ":6190/iotapp/loginSys",
	    				  data:{
	    					  userID : $scope.userid,
	    					  passwd : $scope.password
	    				  }
	    			  }
	    	  ).success(function(data, status){
	    		  //alert(data.LoginResultCode);
	    		  //alert(status)
	    		  //var respObj = JSON.parse(data);
	    		  //window.alert(data.LoginResultCode + " : " + data.LoginResultMsg);
	    		  $scope.displayMessage = data.LoginResultMsg;
	    	       
	    		  if(data.LoginResultCode === 'FAIL'){
	    			  $scope.msgStyle = $scope.msgStylePrefix + 'alert-danger'; //alert-danger , alert-success  
	    		  }
	    		  else{
	    			  $scope.msgStyle = $scope.msgStylePrefix + 'alert-success';
	    		  }
	    		  
	    		  $( "#msgBox" ).fadeIn( "slow" );
	    		  
	    		  if(data.LoginResultCode === 'FAIL'){
	    			  console.log("Login fail");
	    			  $scope.uidisable = false;
	    		  }
	    		  else{
	    			  //$location.url('/index.html#dashboard')
	    			  //alert('VVVVVV0116');
	    			  //$location.path('/dashboard');
	    			  window.location.href="/iotapp/admin/dashboard";
	    		  }
	    		  
	    		  
	    		  
	    	  });
	    	  //$scope.instVar = 'Controller Var Test 123456';
	    	  
	    	  //$( "#msgBox" ).fadeIn( "slow" );
	    	  //$( "#msgBox" ).fadeOut( "slow" );
	      };
		/*
		    $scope.chiliSpicy = function() {
		        $scope.spice = 'chili';
		    };
		
		    $scope.jalapenoSpicy = function() {
		        $scope.spice = 'jalapeño';
		    };
		*/
	}]);
	
	                   
	iotApp.controller('dashboardController', ['$scope','$http', '$location', function($scope, $http, $location) {
		//alert('dashboard controller');
		console.log('in dashboardController......');
		$scope.pageClass = 'dashboard-page';
		 dashboard_initialize();
	}]);
})(window.angular);