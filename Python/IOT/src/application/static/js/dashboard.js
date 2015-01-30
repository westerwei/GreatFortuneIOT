/**
 * 
 */

						
				
			var gauges = [];
			var wsConn;
			//var remoteHostIP = "192.168.1.101";
			//var remoteHostIP = "172.20.10.7";
			
			var context;

			var tempc ,tempf ,humidity;
			var tempcVal = 0, tempfVal = 31, humidityVal = 0;
			var url = "http://" + remoteHostIP + ":6190/iotapp/deviceData/";
		
		
		
			function checkMediaWSConn(){
		    	  
		    	  console.log("Start to check Media notification Web Socket connection....");
		    	  //connection = new WebSocket('ws://192.168.1.11:1150/websocket/addmedia');
		    	  wsConn = new WebSocket('ws://' + remoteHostIP + ':1150/iotapp/ws/devicedatanotify');
		    	  // When the connection is open, send some data to the server
		    	  wsConn.onopen = function () {
		    	    wsConn.send('ping'); // Send the message 'Ping' to the server
		    	    //alert("Connection has been created!");
		    	  };
		
		    	  // Log errors
		    	  wsConn.onerror = function (error) {
		    	    console.log('WebSocket Error ' + error);
		    	  };
		
		    	  // Log messages from the server
		    	  wsConn.onmessage = function (e) {
		    	      if(e.data === "ping_ok" ){
		    	    	  console.log("Web Socket connection runs normal: " + e.data);
		    	    	  //alert("Call back: " + e.data);
		    	      }
		    	      else{
		    	        //console.log("Get data: " + e.data);
		    	        var deviceData = $.parseJSON(e.data);
		    	        var svcType = deviceData.serviceType;
		    	        var date = new Date(deviceData.timestamp.$date)
		    	        console.log("check JSON obj: " + deviceData.timestamp.$date + " : " + date.toUTCString());
		    	        
		    	        if(svcType === 'TEMService'){
		    	        	var tempc = Number(deviceData.DeviceData.Temperature_C);
		    	        	var tempf = Number(deviceData.DeviceData.Temperature_F);
		    	        	//console.log("Temp C: " + tempc);
		    	        	updateGauges(svcType + 'C', tempc);
		    	        	//console.log("Temp F: " + tempf);
		    	        	updateGauges(svcType + 'F', tempf);
		    	        }
		    	        else if(svcType === 'HUMService'){
		    	        	var humidity = Number(deviceData.DeviceData.Humidity);
		    	        	//console.log("Humidity: " + humidity);
		    	        	updateGauges(svcType, humidity);
		    	        }
		    	        
		    	      }
		    		    
		    	  };
		    	  
		      }
			
			function createGauge(name, label, min, max, fontSize)
			{
				var config = 
				{
					size: 150,
					label: label,
					min: undefined != min ? min : 0,
					max: undefined != max ? max : 100,
					minorTicks: 5,
					fontSize: undefined != fontSize ? fontSize : 13
				}
				
				var range = config.max - config.min;
				config.yellowZones = [{ from: config.min + range*0.75, to: config.min + range*0.9 }];
				config.redZones = [{ from: config.min + range*0.9, to: config.max }];
				
				gauges[name] = new Gauge(name + "Container", config);
				gauges[name].render();
			}
			
			function createGauges()
			{
				createGauge("TEMServiceC", "", 0, 60, 13);
				createGauge("TEMServiceF", "", 31, 140, 13);
				createGauge("HUMService", "", 0, 100, 9 );
				//createGauge("test", "Test", -50, 50 );
			}
			
			function updateGauges(key, value)
			{
				gauges[key].redraw(value);
				/* 
				for (var key in gauges)
				{
					var value = getRandomValue(gauges[key])
					gauges[key].redraw(value);
				}
				 */
			}
			
			/* 
			function getRandomValue(gauge)
			{
				var overflow = 0; //10;
				return gauge.config.min - overflow + (gauge.config.max - gauge.config.min + overflow*2) *  Math.random();
			}
			 */
			
			function dashboard_initialize()
			{
				createGauges();
				context = cubism.context()
			    .serverDelay(0)
			    .clientDelay(0)
			    .step(1e3)
			    .size(1024);
				tempc = random("Temperature C");
			    tempf = random("Temperature F");
			    humidity = random("Humidity(%)");
			    var tempcVal = 0, tempfVal = 31, humidityVal = 0;
				//setInterval(updateGauges, 5000);
			    
				console.log("document is ready~~");
		    	checkMediaWSConn();
		    	initialize()
		    	

				d3.select("#series").call(function(div) {
	
				  div.append("div")
				      .attr("class", "axis")
				      .call(context.axis().orient("top"));
	
				  div.selectAll(".horizon")
				      .data([tempc, tempf, humidity])
				    .enter().append("div")
				      .attr("class", "horizon")
				      .call(context.horizon().extent([10, 150]));
	
				  div.append("div")
				      .attr("class", "rule")
				      .call(context.rule());
	
				});
			}
			
			function random(name) {
				  var value = 0,
				      values = [],
				      i = 0,
				      last;
				  return context.metric(function(start, stop, step, callback) {
				    start = +start, stop = +stop;
				    if (isNaN(last)) last = start;
				    //while (last < stop) {
				      last += step;
				      //value = Math.max(-10, Math.min(10, value + .8 * Math.random() - .4 + .2 * Math.cos(i += .2)));
				      //value = Math.max(-10, Math.min(10, value + .8 * 23 - .4 + .2 * Math.cos(i += .2)));
				      value = 0;
				      
				      if(name === 'Temperature C'){
				    	  
				    	  $.getJSON(url + 'tempc')
				    		    .done(function( data ) {
				    		    	tempcVal = data.value
				    		    });
				    		
				    	  
				    	  value = tempcVal;
				      }
				      else if(name === 'Temperature F'){
				    	  $.getJSON(url + 'tempf')
			    		    .done(function( data ) {
			    		    	tempfVal = data.value
			    		    });
				    	  
				    	  value = tempfVal;
				      }
				      else if(name === 'Humidity(%)'){
				    	  $.getJSON(url + 'humidity')
			    		    .done(function( data ) {
			    		    	humidityVal = data.value
			    		    });
				    	  value = humidityVal;
				      }
				      console.log("Value: " + value);  
				      values.push(value);
				    //}
				    callback(null, values = values.slice((start - stop) / step));
				  }, name);
			}
			

		/*
		$( window ).resize(function() {
			  console.log("width: " + $( window ).width());
			});
		*/
