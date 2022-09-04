# pcd-assignment-03
Third assignment for Concurrent and Distributed Programming

## Exercise01
### Description
The application allows to execute different simulations concerning the gravitational forces between multiple bodies. The
can be executed in two different modes:
- Application `with gui`: shows the state of a single simulation as it develops.
- Application `without gui`: tests the performance of different simulation settings,
  showing their execution times.
### How to use
To start the application with gui, run `<project-root>/ex-01/src/main/java/App.java`.\
To start the application without gui, run `<project-root>/ex-01/src/main/java/SimulationBenchmark.java`

## Exercise02
### Description
The application allows to monitor the state of different cities in a cluster, where a city is divided into zones,
containing some pluviometers and a fire-station which takes care of the zone.\
In each zone, an alarm can be triggered if the readings of the majority of the pluviometers exceed a certain threshold. 
An alarm can be deactivated automatically by the fire-station monitoring the zone after a certain amount of time, or 
manually via the frontend connected to that city (only if the zone is being monitored by a fire-station).

### How to use
- First, start the cluster by running `<project-root>/ex-02/src/main/scala/StartCluster.scala`. This should only be run 
  once.
- Then, create as many city as you want in that cluster by running `<project-root>/ex-02/src/main/scala/CreateCity.scala`
  the desired amount of times. 
  When created, each city will print its initialization snapshot, containing the `identifier` of the city.
- Finally, start the graphical user interface by running `<project-root>/ex-02/src/main/scala/Gui.scala`
- Once the gui is started, connect to a city by inserting its `identifier` in order to display its state. Click a specific
  entity in the city to monitor its state as the city develops.

### Examples
#### 1 City - 1 View
Try creating `one` city and starting `one` view.\
Connect the view to the city and watch as it monitors and displays the state
of the city.
#### 1 City - N Views
Try creating `one` city and starting `multiple` views.\
Connect those views to the city and watch as they monitor and display the state of the same city.
#### M City - N Views
Try creating `multiple` cities and starting `multiple` views.\
Connect some of those views to some cities and watch as they monitor and display the state of different cities in the 
same cluster.

### Further notes

- You can configure the application by modifying `<project-root>/ex-02/src/main/scala/configuration/C.scala`
- You can find the log of the cluster over multiple runs in `<project-root>/target/cluster-logging/cluster.log`