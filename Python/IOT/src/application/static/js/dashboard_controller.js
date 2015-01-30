/**
 * AngularJS script for IOTApp module 
 */
//var remoteHostIP = "192.168.1.101";
(function(angular) {
	'use strict';
	//define our application and pull in ngRoute and ngAnimate
	var dashboardPage = angular.module('dashboard-page', ['ngRoute', 'ngAnimate']);
	//DashboardController
	
	dashboardPage.controller('DashboardController', ['$scope','$http', '$location', function($scope, $http, $location) {
		console.log('AngularJS dashboard controller......');
		$scope.testvar2 = '0120-2';
		$scope.headerDivHide = true;
		$scope.deviceList=[];
		
		$scope.getDeviceList = function(custId, userId){
			$http(
	    			  {
	    				  method: "GET",
	    				  url: "http://" + remoteHostIP + ":6190/iotapp/deviceData/" + custId + "/" + userId
	    			  }
	    	  ).success(function(data, status){
	    		  if(data.errorMsg){
	    			  console.log("Fail to get device list.....");
	    		  }
	    		  else{
	    			  console.log("Successful to get device list..... " + data);
	    			  $scope.deviceList = data;
	    		  }
	    	  });
		};
	}]);
})(window.angular);