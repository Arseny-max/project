import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.awt.geom.Ellipse2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MyPanel extends JPanel implements MouseListener, KeyEventDispatcher, MouseWheelListener, MouseMotionListener{
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Point> shell = new ArrayList<>();
    private ArrayList<Circle> circles = new ArrayList<>();
    private ArrayList<Circle> goodcircles = new ArrayList<>();
    private ArrayList<Circle> win = new ArrayList<>();
    private JPanel controlPanel;
    private JTextField xField, yField;
    private double startX;
    private double startY;


    public MyPanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        createControlPanel();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setPreferredSize(new Dimension(200, 60));
        xField = new JTextField(5);
        xField.setPreferredSize(new Dimension(50, 25));
        yField = new JTextField(5);
        yField.setPreferredSize(new Dimension(50, 25));

        JButton addButton = new JButton("Добавить точку");
        addButton.addActionListener(e -> addPointFromFields());

        JButton fileButton = new JButton("Открыть файл");
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));
                int result = fileChooser.showOpenDialog(MyPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        loadPointsFromFile(selectedFile);
                        updateShell();
                        findcircle();
                        repaint();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(MyPanel.this, "Файл не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        controlPanel.add(fileButton);

        controlPanel.add(new JLabel("X:"));
        controlPanel.add(xField);
        controlPanel.add(new JLabel("Y:"));
        controlPanel.add(yField);
        controlPanel.add(addButton);
        controlPanel.setBorder(BorderFactory.createTitledBorder(""));

        this.setLayout(new BorderLayout());
        this.add(controlPanel, BorderLayout.NORTH);
    }
    private void loadPointsFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Pattern pattern = Pattern.compile("(\\d+)\\s+(\\d+)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    double x = Integer.parseInt(matcher.group(1));
                    double y = Integer.parseInt(matcher.group(2));
                    points.add(new Point(x,y));
                } else {
                    System.err.println("Skipping invalid line format: " + line);
                }
            }
        }
    }
    private void addPointFromFields() {
        try {
            double x = Double.parseDouble(xField.getText());
            double y = Double.parseDouble(yField.getText());
            points.add(new Point(x, y));
            updateShell();
            findcircle();
            repaint();
            xField.setText("");
            yField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неправильный ввод", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        for (Point p : points) {
            g2d.fillOval((int)p.x - 2, (int)p.y - 2, 4, 4);
        }
        g2d.setColor(Color.RED);
        if (shell.size() > 1) {
            for (int i = 0; i < shell.size(); i++) {
                Point p1 = shell.get(i);
                Point p2 = shell.get((i + 1) % shell.size());
                g2d.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }
        g2d.setColor(Color.BLUE);
        if (!win.isEmpty()) {
            Circle c = win.get(0);
            Ellipse2D.Double circle = new Ellipse2D.Double(c.x - c.r, c.y - c.r, 2 * c.r, 2 * c.r);
            g2d.draw(circle);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        points.add(new Point(e.getX(), e.getY()));
        updateShell();
        findcircle();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}


    private void updateShell() {
        if (points.size() < 2) {
            shell.clear();
            return;
        }

        int leftmost = 0;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).x < points.get(leftmost).x) {
                leftmost = i;
            }
        }

        shell.clear();
        int p = leftmost;
        do {
            shell.add(points.get(p));
            int q = (p + 1) % points.size();
            for (int i = 0; i < points.size(); i++) {
                if (orientation(points.get(p), points.get(i), points.get(q)) == 2) {
                    q = i;
                }
            }
            p = q;
        } while (p != leftmost);
    }

    private int orientation(Point p, Point q, Point r) {
        double a = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (a == 0) return 0;
        if (a > 0) {
            return 1;
        } else {
            return 2;
        }
    }
    private void findcircle() {
        win.clear();
        circles.clear();
        goodcircles.clear();
        if(shell.size() < 2){
            return;
        }
        for (int i = 0; i < shell.size(); i++) {
            for (int j = 0; j < shell.size(); j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 0; k < shell.size(); k++) {
                    if (k == j) {
                        continue;
                    }
                    if (i == k) {
                        continue;
                    }
                    double x1 = shell.get(i).x;
                    double y1 = shell.get(i).y;
                    double x2 = shell.get(j).x;
                    double y2 = shell.get(j).y;
                    double x3 = shell.get(k).x;
                    double y3 = shell.get(k).y;
                    if (((x2-x1)*(y3-y1) - (x3-x1)*(y2-y1)) == 0){
                        continue;
                    }
                    double d = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
                    double centerX = ((x1 * x1 + y1 * y1) * (y2 - y3) + (x2 * x2 + y2 * y2) * (y3 - y1) + (x3 * x3 + y3 * y3) * (y1 - y2)) / d;
                    double centerY = ((x1 * x1 + y1 * y1) * (x3 - x2) + (x2 * x2 + y2 * y2) * (x1 - x3) + (x3 * x3 + y3 * y3) * (x2 - x1)) / d;
                    double radius = Math.sqrt(Math.pow(centerX - x1, 2) + Math.pow(centerY - y1, 2));
                    circles.add(new Circle(centerX,centerY,radius));
                }
            }
        }
        for (int i = 0; i < shell.size(); i++) {
            for (int j = 0; j < shell.size(); j++) {
                if (i == j) {
                    continue;
                }
                double x1 = shell.get(i).x;
                double y1 = shell.get(i).y;
                double x2 = shell.get(j).x;
                double y2 = shell.get(j).y;
                double centerX = (x1+x2)/2;
                double centerY = (y1+y2)/2;
                double radius = Math.sqrt((x1-centerX)*(x1-centerX)+(y1-centerY)*(y1-centerY));
                circles.add(new Circle(centerX,centerY,radius));
            }
        }
        for (int i = 0; i < circles.size(); i++){
            if (allinside(shell, circles.get(i))){
                goodcircles.add(new Circle(circles.get(i).x,circles.get(i).y,circles.get(i).r));
            }
        }
        int t = 0;
        for (int i = 0; i < goodcircles.size(); i++){
            if (goodcircles.get(i).r < goodcircles.get(t).r) {
                t = i;
            }
        }
        win.add(new Circle(goodcircles.get(t).x, goodcircles.get(t).y, goodcircles.get(t).r));
    }
    public static boolean allinside(ArrayList<Point> points, Circle circle) {
        for (Point p : points) {
            double distance = distance(p, circle);
            if (distance > circle.r) {
                return false;
            }
        }
        return true;
    }
    private static double distance(Point p, Circle c) {
        return Math.sqrt(Math.pow(p.x - c.x, 2) + Math.pow(p.y - c.y, 2));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {

        return false;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double z = 1.0;
        if (e.getWheelRotation() < 0) {
            z *= 1.1;
        } else {
            z /= 1.1;
        }
        for (Point p : points) {
            double a = p.x;
            double b =p.y;
            p.x = (a-e.getX())*z + e.getX();
            p.y = (b-e.getY())*z + e.getY();
        }
        updateShell();
        findcircle();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        for (Point p : points) {
            double a = p.x;
            double b = p.y;
            p.x = a + e.getX() - startX;
            p.y = b + e.getY() - startY;
        }
        updateShell();
        findcircle();
        repaint();
        startX = 0;
        startY = 0;
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
