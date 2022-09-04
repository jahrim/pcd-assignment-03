package mvc.actor;

import mvc.model.Body;
import mvc.model.Boundary;
import util.math.P2d;
import util.math.V2d;
import mvc.view.SimulationView;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Model a builder for simulations.
 */
public class SimulationBuilder {
    private SimulationView viewer;
    private long maxIterations;
    private double dt;
    private Collection<Body> bodies;
    private Boundary bounds;
    private int numberOfSimulationActors;

    /**
     * @param simulation the specified simulation
     * @return a copy of the specified simulation
     * @apiNote for the viewer of the specified simulation, only a copy of its reference is made.
     */
    public static Simulation buildCopyOf(Simulation simulation){ return new Simulation(simulation); }

    /** @return a simulation with the configuration of this builder. */
    public Simulation build() { return new Simulation(this.viewer, this.maxIterations, this.dt, this.bodies, this.bounds, this.numberOfSimulationActors); }

    /**
     * Set the view that will be used to display the state of the simulation over time to the specified mvc.view.
     * @param view the specified view
     * @return this
     */
    public SimulationBuilder setView(SimulationView view){ this.viewer = view; return this; }
    /**
     * Set the bodies of this simulation to the specified bodies.
     * @param bodies the specified bodies
     * @return this
     */
    public SimulationBuilder setBodies(Collection<Body> bodies){ this.bodies = bodies; return this; }
    /**
     * Set the boundary of this simulation to the specified boundary.
     * @param bounds the specified boundary
     * @return this
     */
    public SimulationBuilder setBoundary(Boundary bounds){ this.bounds = bounds; return this; }
    /**
     * Set the amount of time that passes at each completed iteration in this simulation to the specified duration.
     * @param dt the specified duration
     * @return this
     */
    public SimulationBuilder setDeltaTime(double dt){ this.dt = dt; return this; }
    /**
     * Set the amount of iterations that this simulation will execute before stopping to the specified amount.
     * @param maxIterations the specified amount of iterations
     * @return this
     */
    public SimulationBuilder setMaxIterations(long maxIterations){ this.maxIterations = maxIterations; return this; }
    /**
     * Set the number of simulation actors spawned by the simulation director to the specified number.
     * @param numberOfSimulationActors the specified number of simulation actors
     * @return this
     */
    public SimulationBuilder setNumberOfSimulationActors(int numberOfSimulationActors){ this.numberOfSimulationActors = numberOfSimulationActors; return this; }

    /** Test with two bodies, one with double the mass of the other. */
    public SimulationBuilder testBodySet1_two_bodies() {
        this.bounds = new Boundary(-4.0, -4.0, 4.0, 4.0);
        this.bodies = new ArrayList<>();
        this.bodies.add(new Body(0, new P2d(-0.1, 0), new V2d(0,0), new V2d(0,0), 1));
        this.bodies.add(new Body(1, new P2d(0.1, 0), new V2d(0,0), new V2d(0,0), 2));
        return this;
    }

    /** Test with three bodies, one with ten times the mass of the others. */
    public SimulationBuilder testBodySet2_three_bodies() {
        this.bounds = new Boundary(-1.0, -1.0, 1.0, 1.0);
        this.bodies = new ArrayList<>();
        this.bodies.add(new Body(0, new P2d(0, 0), new V2d(0,0), new V2d(0,0), 10));
        this.bodies.add(new Body(1, new P2d(0.2, 0), new V2d(0,0), new V2d(0,0), 1));
        this.bodies.add(new Body(2, new P2d(-0.2, 0), new V2d(0,0), new V2d(0,0), 1));
        return this;
    }

    /** Test with some bodies, all with the same mass. */
    public SimulationBuilder testBodySet3_some_bodies() {
        this.bounds = new Boundary(-4.0, -4.0, 4.0, 4.0);
        int nBodies = 100;
        Random rand = new Random(System.currentTimeMillis());
        this.bodies = new ArrayList<>();
        for (int i = 0; i < nBodies; i++) {
            double x = this.bounds.getX0()*0.25 + rand.nextDouble() * (this.bounds.getX1() - this.bounds.getX0()) * 0.25;
            double y = this.bounds.getY0()*0.25 + rand.nextDouble() * (this.bounds.getY1() - this.bounds.getY0()) * 0.25;
            Body b = new Body(i, new P2d(x, y), new V2d(0, 0), new V2d(0,0), 10);
            this.bodies.add(b);
        }
        return this;
    }

    /** Test with a lot of bodies, all with the same mass. */
    public SimulationBuilder testBodySet4_many_bodies() {
        this.bounds = new Boundary(-6.0, -6.0, 6.0, 6.0);
        int nBodies = 1000;
        Random rand = new Random(System.currentTimeMillis());
        this.bodies = new ArrayList<>();
        for (int i = 0; i < nBodies; i++) {
            double x = this.bounds.getX0()*0.25 + rand.nextDouble() * (this.bounds.getX1() - this.bounds.getX0()) * 0.25;
            double y = this.bounds.getY0()*0.25 + rand.nextDouble() * (this.bounds.getY1() - this.bounds.getY0()) * 0.25;
            Body b = new Body(i, new P2d(x, y), new V2d(0, 0), new V2d(0,0), 10);
            this.bodies.add(b);
        }
        return this;
    }

    /**
     * Test with the specified amount of bodies, all with the same mass.
     * @param nBodies the specified amount of bodies
     * @return this
     */
    public SimulationBuilder testCustomBodySet(int nBodies) {
        this.bounds = new Boundary(-10.0, -10.0, 10.0, 10.0);
        Random rand = new Random(System.currentTimeMillis());
        this.bodies = new ArrayList<>();
        for (int i = 0; i < nBodies; i++) {
            double x = this.bounds.getX0()*0.25 + rand.nextDouble() * (this.bounds.getX1() - this.bounds.getX0()) * 0.25;
            double y = this.bounds.getY0()*0.25 + rand.nextDouble() * (this.bounds.getY1() - this.bounds.getY0()) * 0.25;
            Body b = new Body(i, new P2d(x, y), new V2d(0, 0), new V2d(0,0), 10);
            this.bodies.add(b);
        }
        return this;
    }

    /**
     * Model a simulation of some bodies within a certain boundary.
     */
    public static class Simulation {
        /** The view that will be used to display the state of the simulation over time. */
        public final SimulationView viewer;
        /** The amount of iterations that this simulation will execute before stopping. */
        public final long maxIterations;
        /** The amount of time that passes at each completed iteration in this simulation. */
        public final double dt;
        /** The bodies of this simulation. */
        public final Map<Integer, Body> bodies;
        /** The boundary of this simulation. */
        public final Boundary bounds;
        /** The number of simulation actors. */
        public final int numberOfSimulationActors;

        private long currentIteration;
        private double virtualTime;

        private final Collection<Consumer<Long>> onIterationCompleted;

        private Simulation(SimulationView viewer, long maxIterations, double dt, Collection<Body> bodies, Boundary bounds, int numberOfSimulationActors) {
            this.viewer = viewer;
            this.maxIterations = maxIterations;
            this.dt = dt;
            this.bodies = bodies.stream().collect(Collectors.toMap(Body::getId, x -> x));
            this.bounds = bounds;
            this.currentIteration = 0;
            this.virtualTime = 0;
            this.numberOfSimulationActors = numberOfSimulationActors;
            this.onIterationCompleted = new LinkedList<>();
        }
        private Simulation(Simulation simulation){
            this(
                simulation.viewer,
                simulation.maxIterations,
                simulation.dt,
                simulation.bodies.values().stream().map(Body::copyOf).collect(Collectors.toList()),
                Boundary.copyOf(simulation.bounds),
                simulation.numberOfSimulationActors
            );
            this.currentIteration = simulation.currentIteration;
            this.virtualTime = simulation.virtualTime;
            this.onIterationCompleted.addAll(simulation.onIterationCompleted);
        }
        /** @return a copy of this simulation. */
        public Simulation getSnapshot() { return SimulationBuilder.buildCopyOf(this); }

        /** @return the amount of time that passed since the beginning of the simulation. */
        public double getVirtualTime(){ return this.virtualTime; }
        /** @return the current iteration of this simulation. */
        public long getCurrentIteration(){ return this.currentIteration; }
        /** @return true if this simulation has a viewer attached, false otherwise. */
        public boolean hasViewer() { return this.viewer != null; }
        /** @return true if this simulation is not ended, false otherwise. */
        public boolean isRunning() { return this.currentIteration < this.maxIterations; }

        /**
         * Adds the specified callback to the handlers to be executed when this simulation completes an iteration.
         * @param callback the specified callback, consuming the number of the current iteration
         * @return this
         */
        public Simulation onIterationComplete(Consumer<Long> callback){ this.onIterationCompleted.add(callback); return this; }
        /**
         * Increase the amount of time that passed since the beginning of the simulation
         * by the duration of an iteration.
         * @return this
         */
        public Simulation completeIteration(){
            this.currentIteration++;
            this.virtualTime += this.dt;
            this.onIterationCompleted.forEach(callback -> callback.accept(this.currentIteration));
            return this;
        }
        /**
         * Replaces some bodies in the simulation with the specified bodies.
         * If an updated body has an id that is present in the simulation, the corresponding
         * body will be substituted with the new one, otherwise the updated body will be added
         * to the simulation.
         * @param updatedBodies the specified bodies
         * @return this
         */
        public Simulation updateBodies(Collection<Body> updatedBodies){
            updatedBodies.forEach(body -> this.bodies.put(body.getId(), body));
            return this;
        }
        /**
         * Updates the view of this simulation if any viewer is attached to it.
         * @return this
         */
        public Simulation updateView(){
            if (this.hasViewer()){ this.viewer.display(this); }
            return this;
        }
    }
}