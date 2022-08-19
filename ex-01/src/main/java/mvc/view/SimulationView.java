package mvc.view;

import mvc.actor.SimulationBuilder;
import mvc.actor.SimulationDirector;
import akka.actor.typed.ActorRef;

/**
 * Simulation mvc.view.
 */
public class SimulationView {
	private final VisualiserFrame frame;
	
    /**
     * Creates a view of the specified size (in pixels)
     * @param w the specified width
     * @param h the specified height
     */
    public SimulationView(int w, int h){
    	frame = new VisualiserFrame(w,h);
    }
        
    public void display(SimulationBuilder.Simulation simulation){
 	   frame.display(simulation);
    }

    /**
     * Attach the specified simulation director to this viewer.
     * @param director the specified simulation director
     */
    public void attachSimulationDirector(ActorRef<SimulationDirector.SimulationDirectorMessage> director){
        this.frame.attachSimulationDirector(director);
    }
}