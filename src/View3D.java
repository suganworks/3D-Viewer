import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class View3D extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

	private static final long serialVersionUID = 1L;

	private static final int WIDTH = 800, HEIGHT = 800;

    private double rotX = 0, rotY = 0;
    private double zoom = 1.5;
    private boolean wireframe = false;

    private Point lastDrag;

    private String currentModel = "Cube";
    private Color modelColor = Color.BLUE;

    private final Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA};
    private int colorIndex = 0;

    public View3D() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        setFocusable(true);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple 3D Viewer");
        View3D panel = new View3D();

        JComboBox<String> modelSelector = new JComboBox<>(new String[]{"Cube", "Pyramid", "Sphere"});
        modelSelector.addActionListener(e -> {
            panel.currentModel = (String) modelSelector.getSelectedItem();
            panel.repaint();
        });

        JButton colorButton = new JButton("Change Color");
        colorButton.addActionListener(e -> {
            panel.colorIndex = (panel.colorIndex + 1) % panel.colors.length;
            panel.modelColor = panel.colors[panel.colorIndex];
            panel.repaint();
        });

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Model:"));
        topPanel.add(modelSelector);
        topPanel.add(colorButton);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        ArrayList<Triangle> triangles;

        switch (currentModel) {
            case "Cube" -> triangles = createCube(modelColor);
            case "Pyramid" -> triangles = createPyramid(modelColor);
            case "Sphere" -> triangles = createSphere(modelColor, 20, 20);
            default -> triangles = createCube(modelColor);
        }

        Graphics2D g2 = (Graphics2D) g;
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        for (Triangle t : triangles) {
            // Rotate points
            Point3D p1 = t.p1.rotateX(rotX).rotateY(rotY).scale(zoom);
            Point3D p2 = t.p2.rotateX(rotX).rotateY(rotY).scale(zoom);
            Point3D p3 = t.p3.rotateX(rotX).rotateY(rotY).scale(zoom);

            // Backface culling
            Point3D u = p2.subtract(p1);
            Point3D v = p3.subtract(p1);
            Point3D normal = u.cross(v);
            if (normal.z >= 0) continue;

            // Project to 2D
            int x1 = (int) (cx + p1.x);
            int y1 = (int) (cy - p1.y);
            int x2 = (int) (cx + p2.x);
            int y2 = (int) (cy - p2.y);
            int x3 = (int) (cx + p3.x);
            int y3 = (int) (cy - p3.y);

            if (wireframe) {
                g2.setColor(t.color.darker());
                g2.drawLine(x1, y1, x2, y2);
                g2.drawLine(x2, y2, x3, y3);
                g2.drawLine(x3, y3, x1, y1);
            } else {
                Polygon poly = new Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
                g2.setColor(t.color);
                g2.fillPolygon(poly);
                g2.setColor(Color.BLACK);
                g2.drawPolygon(poly);
            }
        }
    }

    // Cube with 12 triangles
    private ArrayList<Triangle> createCube(Color color) {
        ArrayList<Triangle> tris = new ArrayList<>();

        Point3D[] vertices = {
                new Point3D(-1, -1, -1),
                new Point3D(1, -1, -1),
                new Point3D(1, 1, -1),
                new Point3D(-1, 1, -1),
                new Point3D(-1, -1, 1),
                new Point3D(1, -1, 1),
                new Point3D(1, 1, 1),
                new Point3D(-1, 1, 1),
        };

        // front face
        tris.add(new Triangle(vertices[0], vertices[1], vertices[2], color));
        tris.add(new Triangle(vertices[2], vertices[3], vertices[0], color));

        // right face
        tris.add(new Triangle(vertices[1], vertices[5], vertices[6], color));
        tris.add(new Triangle(vertices[6], vertices[2], vertices[1], color));

        // back face
        tris.add(new Triangle(vertices[5], vertices[4], vertices[7], color));
        tris.add(new Triangle(vertices[7], vertices[6], vertices[5], color));

        // left face
        tris.add(new Triangle(vertices[4], vertices[0], vertices[3], color));
        tris.add(new Triangle(vertices[3], vertices[7], vertices[4], color));

        // top face
        tris.add(new Triangle(vertices[3], vertices[2], vertices[6], color));
        tris.add(new Triangle(vertices[6], vertices[7], vertices[3], color));

        // bottom face
        tris.add(new Triangle(vertices[1], vertices[0], vertices[4], color));
        tris.add(new Triangle(vertices[4], vertices[5], vertices[1], color));

        return tris;
    }

    // Pyramid with 6 triangles
    private ArrayList<Triangle> createPyramid(Color color) {
        ArrayList<Triangle> tris = new ArrayList<>();

        Point3D base0 = new Point3D(-1, -1, -1);
        Point3D base1 = new Point3D(1, -1, -1);
        Point3D base2 = new Point3D(1, -1, 1);
        Point3D base3 = new Point3D(-1, -1, 1);
        Point3D apex = new Point3D(0, 1, 0);

        // base
        tris.add(new Triangle(base0, base1, base2, color));
        tris.add(new Triangle(base2, base3, base0, color));

        // sides
        tris.add(new Triangle(base0, base1, apex, color));
        tris.add(new Triangle(base1, base2, apex, color));
        tris.add(new Triangle(base2, base3, apex, color));
        tris.add(new Triangle(base3, base0, apex, color));

        return tris;
    }

    // Sphere generation with latitude/longitude triangles
    private ArrayList<Triangle> createSphere(Color color, int latSteps, int lonSteps) {
        ArrayList<Triangle> tris = new ArrayList<>();

        for (int lat = 0; lat < latSteps; lat++) {
            double lat0 = Math.PI * (-0.5 + (double) lat / latSteps);
            double lat1 = Math.PI * (-0.5 + (double) (lat + 1) / latSteps);

            double y0 = Math.sin(lat0);
            double y1 = Math.sin(lat1);

            double r0 = Math.cos(lat0);
            double r1 = Math.cos(lat1);

            for (int lon = 0; lon < lonSteps; lon++) {
                double lon0 = 2 * Math.PI * (double) lon / lonSteps;
                double lon1 = 2 * Math.PI * (double) (lon + 1) / lonSteps;

                Point3D p1 = new Point3D(r0 * Math.cos(lon0), y0, r0 * Math.sin(lon0));
                Point3D p2 = new Point3D(r1 * Math.cos(lon0), y1, r1 * Math.sin(lon0));
                Point3D p3 = new Point3D(r1 * Math.cos(lon1), y1, r1 * Math.sin(lon1));
                Point3D p4 = new Point3D(r0 * Math.cos(lon1), y0, r0 * Math.sin(lon1));

                // Two triangles per quad
                tris.add(new Triangle(p1, p2, p3, color));
                tris.add(new Triangle(p3, p4, p1, color));
            }
        }

        return tris;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastDrag != null) {
            int dx = e.getX() - lastDrag.x;
            int dy = e.getY() - lastDrag.y;

            rotY += dx * 0.01;
            rotX += dy * 0.01;

            repaint();
        }
        lastDrag = e.getPoint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastDrag = e.getPoint();
        requestFocusInWindow();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastDrag = null;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom -= e.getPreciseWheelRotation() * 0.1;
        if (zoom < 0.1) zoom = 0.1;
        if (zoom > 5) zoom = 5;
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> {
                wireframe = !wireframe;
                repaint();
            }
            case KeyEvent.VK_R -> {
                rotX = 0;
                rotY = 0;
                zoom = 1.5;
                repaint();
            }
        }
    }

    // Unused interface methods
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    // Helper classes
    private static class Point3D {
        double x, y, z;
        public Point3D(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
        Point3D rotateX(double angle) {
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double ny = y * cos - z * sin;
            double nz = y * sin + z * cos;
            return new Point3D(x, ny, nz);
        }
        Point3D rotateY(double angle) {
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double nx = x * cos + z * sin;
            double nz = -x * sin + z * cos;
            return new Point3D(nx, y, nz);
        }
        Point3D scale(double s) {
            return new Point3D(x * s * 100, y * s * 100, z * s * 100);
        }
        Point3D subtract(Point3D o) {
            return new Point3D(x - o.x, y - o.y, z - o.z);
        }
        Point3D cross(Point3D o) {
            return new Point3D(
                    y * o.z - z * o.y,
                    z * o.x - x * o.z,
                    x * o.y - y * o.x
            );
        }
    }

    private static class Triangle {
        Point3D p1, p2, p3;
        Color color;
        public Triangle(Point3D p1, Point3D p2, Point3D p3, Color c) {
            this.p1 = p1; this.p2 = p2; this.p3 = p3; this.color = c;
        }
    }
}
