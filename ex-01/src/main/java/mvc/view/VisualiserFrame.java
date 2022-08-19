package mvc.view;

import mvc.actor.SimulationBuilder;
import mvc.actor.SimulationDirector;
import akka.actor.typed.ActorRef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VisualiserFrame extends JFrame {
    private ActorRef<SimulationDirector.SimulationDirectorMessage> director;

    private final VisualiserPanel panel;
    private final JButton startButton, stopButton;

    public VisualiserFrame(int w, int h){
        setTitle("Bodies Simulation");
        setSize(w,h);
        setResizable(false);

        JPanel controlPanel = new JPanel();
        this.startButton = new JButton("start");
        this.stopButton = new JButton("stop");
        controlPanel.add(this.startButton);
        controlPanel.add(this.stopButton);

        this.panel = new VisualiserPanel(w,h);

        JPanel mainPanel = new JPanel();
        LayoutManager layout = new BorderLayout();
        mainPanel.setLayout(layout);
        mainPanel.add(BorderLayout.SOUTH, controlPanel);
        mainPanel.add(BorderLayout.CENTER, this.panel);
        setContentPane(mainPanel);

        startButton.setEnabled(false);
        startButton.addActionListener(ev -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            director.tell(new SimulationDirector.StartMessage());
        });

        stopButton.setEnabled(false);
        stopButton.addActionListener(ev -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            director.tell(new SimulationDirector.StopMessage());
        });

        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent ev){
                System.out.println("Window closing..."); System.exit(0);
            }
            public void windowClosed(WindowEvent ev){
                System.out.println("Window closed..."); System.exit(0);
            }
        });
        this.setVisible(true);
    }

    public void display(SimulationBuilder.Simulation simulation) {
        try {
            SwingUtilities.invokeLater(() -> {
                panel.display(simulation);
                repaint();
            });
        } catch (Exception ignored) {}
    }

    public void updateScale(double k) {
        panel.updateScale(k);
    }
    public void attachSimulationDirector(ActorRef<SimulationDirector.SimulationDirectorMessage> director){
        this.director = director;
        this.startButton.setEnabled(true);
    }
}