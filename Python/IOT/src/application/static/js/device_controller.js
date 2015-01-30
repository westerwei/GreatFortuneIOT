/**
 * AngularJS script for IOTApp module 
 */
//var remoteHostIP = "192.168.1.101";
(function(angular) {
	'use strict';
	//define our application and pull in ngRoute and ngAnimate
	var devicePage = angular.module('device-page', ['ngRoute', 'ngAnimate']);
	//DashboardController
	
	devicePage.directive('enableBtnTooltip', function() {
		 return function(scope, element, attrs) {
			    if (scope.$last){
			    	$("button").tooltip();
			    }
			  };
	});
	
	devicePage.controller('DeviceController', ['$scope','$http', '$location', '$timeout', function($scope, $http, $location, $timeout) {
		console.log('AngularJS Device controller......');
		
		$scope.headerDivHide = true;
		$scope.deviceList=[];
		$scope.selectedDeviceIndex = -1;
		$scope.deviceAccordionState = new Array();
		
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
	    			  for(var i=0;i<data.length;i++){
	    				  $scope.deviceAccordionState["device" + i] = false;
	    			  }
	    			  $scope.deviceList = data;
	    		  }
	    	  });
		};
		
		
		$scope.expandDeviceDataPanel = function(deviceIndex){
			//alert("expandDeviceDataPanel: " + deviceIndex + " | " + $scope.deviceAccordionState["device" + deviceIndex]);
			var accordionState = $scope.deviceAccordionState["device" + deviceIndex];
			if(accordionState){
				//alert("Close data panel!");
				$scope.deviceAccordionState["device" + deviceIndex] = false;
				return;
			}
			$scope.selectedDeviceIndex = deviceIndex;
			$("#dimmerContainer" + deviceIndex).fadeIn("slow");
			
			$timeout($scope.loadDeviceData,2000);
			
			$scope.deviceAccordionState["device" + deviceIndex] = true;
			
		};
		
		$scope.loadDeviceData = function(){
			//alert($scope.selectedDeviceIndex);
			$("#deviceSerialNO" + $scope.selectedDeviceIndex).val("Load Test");
			
			
			$("#dimmerContainer" + $scope.selectedDeviceIndex).fadeOut("slow");
		};
		
		$scope.editDeviceData = function(deviceIndex){
			$("#deviceFormField" + deviceIndex).prop("disabled", false);
			
			
		};
		
		$scope.saveDeviceData = function(deviceIndex){
			
			
		};
		
		$scope.resetDeviceDataPanel = function(deviceIndex){
			
			
		};		
		
	}]);
})(window.angular);