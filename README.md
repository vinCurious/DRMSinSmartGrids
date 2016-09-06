# DRMSinSmartGrids
Demand Response Management System in Smart Grids

Technical Paper Link: https://drive.google.com/open?id=0BzHtvANDgsA9RGZxTi0xa3FSWVU

Smart Grid focuses to fulfil increasing demand for electricity, by providing reliability, efficiency and sustainability. Grid which is self-healing, self-balancing and self-optimizing long distance transmission, and automated monitoring and analysis tools based on real-time data about weather, outage history, etc. This system can manage demand response actions either by decreasing demand through reduced voltage on the distribution network or regulators can ask consumers to change their electricity consumption patterns when supply is short.

Smart grid supports two-way traffic between electricity supplier and consumer, that means it takes response from the consumer to make the service more reliable and minimize the possibility of power disruptions. Demand response system consists of sensors for sensing the excessive load on the station and based on the system decision, it can either deny the services or divert the load to most strategically located station.

Here, we are studying historical data of power demands and power consumption patterns from households and businesses after fixed interval of time to reset the grid setup. 

Implementation:
We have implemented demand response management with distribution intelligence. Our implementation requires peak values of power requirement on biweekly basis from all connected households and companies per station, which consists of hourly based peak consumption values. We then analyze the trend from this historical data for peak values. We calculate average peak values per hour for all households or industries included. 
These input files are collected per station and new average peak values are set per day per hour for respective stations.
e.g. for Station 101, we have set following average peak values
Saturday, 9:377
Wednesday, 24:372
Friday, 20:385
Wednesday, 3:413
Friday, 24:314

Whenever consumer requests power supply from the power station, it redirects the request to the demand response system, where the system makes decision based on the peak value for given location at given hour. If the value is within the threshold limit, then system gives green signal to the grid to send the requested amount of electrical energy, else if the value exceeds the peak limit, then the system serves the request with a warning message to the consumer for exceeding the threshold limit at given time (exceeding consumption can be charged more, for each unit of electricity after a particular limit). If the current usage reaches the threshold value of any particular station, the station then borrows energy from other stations.

Types of requests:
1. Withdraw Request – Current station broadcasts this type pf request in case it had previously lent some power to other stations. This request is sent to these stations only.
2. General request – Current stations broadcasts this to all other stations in case withdraw request didn’t get any response.

Whoever, sends the positive reply to general broadcasted request that station thus lends required power units. New values of capacity and threshold are updated in both the systems as shown in figure 2 i.e. the station which requests and station which lends the power units.

As we have peak value data on biweekly basis, at the end of 2nd week we need to update peak value table, with new peak values observed on the basis of current trend in the demand for power supply and this updated table goes input to demand response system for next cycle.

RESULTS AND CONCLUSION
After running the above implementation for few iterations, we can see the alleviation in the demand of power supply at peak hours, as we are sending warning/response to consumers (analogy to price increment). The utilization of energy at peak hours has decreased by 10-15 percentage. This helps in increasing end users’ awareness towards a more rational use of energy.
