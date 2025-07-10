import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class BFSRoutingGUI extends Application {

    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private Map<String, Double[]> nodePositions = new HashMap<>();
    private Canvas canvas;
    private String selectedNode = null;
    private double offsetX = 0, offsetY = 0;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Input fields
        TextField fromNode = new TextField(); fromNode.setPromptText("From");
        TextField toNode = new TextField(); toNode.setPromptText("To");
        TextField weightField = new TextField(); weightField.setPromptText("Weight");
        Button addEdgeBtn = new Button("Add Edge");

        TextField sourceField = new TextField(); sourceField.setPromptText("Source");
        TextField destField = new TextField(); destField.setPromptText("Destination");
        Button bfsButton = new Button("Run BFS");

        TextField deleteField = new TextField(); deleteField.setPromptText("Node to Delete");
        Button deleteNodeBtn = new Button("Delete Node");

        // Top panel
        HBox inputPanel = new HBox(10,
                new Label("Edge:"), fromNode, toNode, weightField, addEdgeBtn,
                new Label("BFS:"), sourceField, destField, bfsButton,
                deleteField, deleteNodeBtn
        );
        inputPanel.setStyle("-fx-padding: 10;");
        root.setTop(inputPanel);

        // Canvas
        canvas = new Canvas(800, 450);
        root.setCenter(canvas);

        // Log area
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(100);
        root.setBottom(logArea);

        // Add edge
        addEdgeBtn.setOnAction(e -> {
            String from = fromNode.getText().trim();
            String to = toNode.getText().trim();
            int weight;

            try {
                weight = Integer.parseInt(weightField.getText().trim());
            } catch (NumberFormatException ex) {
                weight = 1;
            }

            if (!from.isEmpty() && !to.isEmpty()) {
                graph.putIfAbsent(from, new HashMap<>());
                graph.putIfAbsent(to, new HashMap<>());
                graph.get(from).put(to, weight);

                placeNodeIfAbsent(from);
                placeNodeIfAbsent(to);

                drawGraph(null);
            }
        });

        // Run BFS
        bfsButton.setOnAction(e -> {
            String source = sourceField.getText().trim();
            String dest = destField.getText().trim();
            List<String> path = bfs(source, dest, logArea);
            drawGraph(path);
        });

        // Delete node
        deleteNodeBtn.setOnAction(e -> {
            String nodeToDelete = deleteField.getText().trim();
            if (!graph.containsKey(nodeToDelete)) return;

            graph.remove(nodeToDelete);
            for (String from : graph.keySet()) {
                graph.get(from).remove(nodeToDelete);
            }
            nodePositions.remove(nodeToDelete);
            drawGraph(null);
        });

        // Drag nodes
        canvas.setOnMousePressed(e -> {
            for (String node : nodePositions.keySet()) {
                Double[] pos = nodePositions.get(node);
                double dx = e.getX() - pos[0];
                double dy = e.getY() - pos[1];
                if (Math.hypot(dx, dy) < 15) {
                    selectedNode = node;
                    offsetX = dx;
                    offsetY = dy;
                    break;
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (selectedNode != null) {
                nodePositions.get(selectedNode)[0] = e.getX() - offsetX;
                nodePositions.get(selectedNode)[1] = e.getY() - offsetY;
                drawGraph(null);
            }
        });

        canvas.setOnMouseReleased(e -> selectedNode = null);

        // Final setup
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("JavaFX BFS Visualizer with Weights, Deletion & Drag");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void placeNodeIfAbsent(String node) {
        if (!nodePositions.containsKey(node)) {
            double x = 100 + Math.random() * 600;
            double y = 50 + Math.random() * 350;
            nodePositions.put(node, new Double[]{x, y});
        }
    }

    private void drawGraph(List<String> path) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw edges
        gc.setLineWidth(2);
        for (String from : graph.keySet()) {
            for (String to : graph.get(from).keySet()) {
                Double[] fromPos = nodePositions.get(from);
                Double[] toPos = nodePositions.get(to);
                int weight = graph.get(from).get(to);

                // Edge color: red if in BFS path, else yellow
                gc.setStroke((path != null && isEdgeInPath(path, from, to)) ? Color.RED : Color.GOLD);
                gc.strokeLine(fromPos[0], fromPos[1], toPos[0], toPos[1]);

                // Draw weight label in black
                double midX = (fromPos[0] + toPos[0]) / 2;
                double midY = (fromPos[1] + toPos[1]) / 2;
                gc.setStroke(Color.BLACK);
                gc.strokeText(String.valueOf(weight), midX, midY);
            }
        }

        // Draw nodes
        for (String node : nodePositions.keySet()) {
            Double[] pos = nodePositions.get(node);
            gc.setFill(Color.LIGHTBLUE);
            gc.fillOval(pos[0] - 10, pos[1] - 10, 20, 20);
            gc.setStroke(Color.BLACK);
            gc.strokeText(node, pos[0] - 10, pos[1] - 15);
        }
    }

    private boolean isEdgeInPath(List<String> path, String from, String to) {
        for (int i = 0; i < path.size() - 1; i++) {
            if (path.get(i).equals(from) && path.get(i + 1).equals(to)) return true;
        }
        return false;
    }

    private List<String> bfs(String start, String goal, TextArea logArea) {
        logArea.clear();
        if (!graph.containsKey(start) || !graph.containsKey(goal)) {
            logArea.appendText("Start or goal node does not exist.\n");
            return null;
        }

        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            logArea.appendText("Visiting: " + current + "\n");

            if (current.equals(goal)) break;

            for (String neighbor : graph.get(current).keySet()) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                }
            }
        }

        if (!parent.containsKey(goal)) {
            logArea.appendText("No path found from " + start + " to " + goal + ".\n");
            return null;
        }

        List<String> path = new ArrayList<>();
        String current = goal;
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }

        logArea.appendText("Path found: " + String.join(" → ", path) + "\n");
        return path;
    }

    public static void main(String[] args) {
        launch(args);
    }
}