import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.awt.geom.Ellipse2D;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MyPanel extends JPanel implements MouseListener, KeyEventDispatcher, MouseWheelListener, MouseMotionListener{
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Point> shell = new ArrayList<>();
    private ArrayList<Circle> circles = new ArrayList<>();
    private ArrayList<Circle> enclosingCircles = new ArrayList<>();
    private ArrayList<Circle> minСircle = new ArrayList<>();
    private JTextField xField, yField;
    private double startX;
    private double startY;
    private boolean dragging;
    private boolean deletepoint;
    private  boolean showshell = false;
    private int  show = 0;


    public MyPanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        createButtons();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

private void createButtons() {
    Border bevelBorder = createBevelBorder();
    Border compoundBevel = createCompoundBevel(bevelBorder, 5);
    Border compoundBevelpic = createCompoundBevel(bevelBorder, 0);

    createInputFields();

    this.add(createIconButtonPanel("ластик.png", true));
    this.add(createIconButtonPanel("курсор.png", false));
    this.add(createShowShellButton(compoundBevel));
    this.add(createAddPointPanel(compoundBevel));
    this.add(createFileButtonPanel(compoundBevel));
    this.add(createClearButtonPanel(compoundBevel));
    this.add(createCloseButtonPanel(compoundBevel));
}

    private Border createBevelBorder() {
        return BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.WHITE, Color.GRAY);
    }

    private Border createCompoundBevel(Border border, int padding) {
        return BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)
        );
    }

    private void createInputFields() {
        xField = createTextField(50);
        yField = createTextField(50);
    }

    private JTextField createTextField(int width) {
        JTextField field = new JTextField(5);
        field.setPreferredSize(new Dimension(width, 25));
        return field;
    }

    private Box createIconButtonPanel(String iconPath, boolean isDelete) {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setIcon(new ImageIcon("data/" + iconPath));
        button.addActionListener(e -> deletepoint = isDelete);

        Box box = Box.createHorizontalBox();
        box.setBorder(createCompoundBevel(createBevelBorder(), 0));
        box.add(button);
        return box;
    }

    private Box createAddPointPanel(Border border) {
        Box box = Box.createHorizontalBox();
        box.setBorder(border);
        box.add(new JLabel("X:"));
        box.add(xField);
        box.add(new JLabel("Y:"));
        box.add(yField);
        box.add(createAddButton());
        return box;
    }

    private JButton createAddButton() {
        JButton button = new JButton("Добавить точку");
        button.setFocusPainted(false);
        button.addActionListener(e -> addPointFromFields());
        return button;
    }

    private Box createActionButtonPanel(String title, Border border, ActionListener action) {
        JButton button = new JButton(title);
        button.setFocusPainted(false);
        button.addActionListener(action);

        Box box = Box.createHorizontalBox();
        box.setBorder(border);
        box.add(button);
        return box;
    }

    private Box createFileButtonPanel(Border border) {
        return createActionButtonPanel("Открыть файл", border, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleFileSelection();
            }
        });
    }

    private Box createClearButtonPanel(Border border) {
        return createActionButtonPanel("Удалить всё", border, e -> {
            points.clear();
            shell.clear();
            circles.clear();
            enclosingCircles.clear();
            minСircle.clear();
            repaint();
            Sound.playSound("data/deleteall.wav").setVolume(0.9f);
        });
    }

    private Box createCloseButtonPanel(Border border) {
        return createActionButtonPanel("Закрыть программу", border, e -> System.exit(0));
    }

    private Box createShowShellButton(Border border) {
        JButton button = new JButton("Показать выпуклую оболочку");
        button.setFocusPainted(false);
        button.addActionListener(e -> toggleShellVisibility(button));

        Box box = Box.createHorizontalBox();
        box.setBorder(border);
        box.add(button);
        return box;
    }

    private void toggleShellVisibility(JButton button) {
        show++;
        showshell = (show % 2 == 1);
        button.setText(showshell ? "Убрать выпуклую оболочку" : "Показать выпуклую оболочку");
        repaint();
    }

    private void handleFileSelection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        if (fileChooser.showOpenDialog(MyPanel.this) == JFileChooser.APPROVE_OPTION) {
            try {
                loadPointsFromFile(fileChooser.getSelectedFile());
                updateShell();
                findcircle();
                repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(MyPanel.this,
                        "Файл не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void loadPointsFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)");
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        double x = Double.parseDouble(matcher.group(1));
                        double y = Double.parseDouble(matcher.group(2));
                        points.add(new Point(x, y));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Пропуск неверного формата числа в строке: " + line, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Пропуск недопустимого формата строки. Ожидаются два рациональных числа, разделенных пробелами: " + line, "Ошибка", JOptionPane.ERROR_MESSAGE);
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
        if (showshell == true) {
            g2d.setColor(Color.RED);
            if (shell.size() > 1) {
                for (int i = 0; i < shell.size(); i++) {
                    Point p1 = shell.get(i);
                    Point p2 = shell.get((i + 1) % shell.size());
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                }
            }
        }

        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(Color.BLUE);
        if (!minСircle.isEmpty()) {
            Circle c = minСircle.get(0);
            Ellipse2D.Double circle = new Ellipse2D.Double(c.x - c.r, c.y - c.r, 2 * c.r, 2 * c.r);
            g2d.draw(circle);
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        if (deletepoint == true){
            Circle deletecircle = new Circle(e.getX(), e.getY(), 5);
            Iterator<Point> iterator = points.iterator();
            while(iterator.hasNext()){
                Point point = iterator.next();
                if(distance(point, deletecircle) <= deletecircle.r){
                    Sound.playSound("data/delete.wav").setVolume(0.9f);
                    iterator.remove();
                    updateShell();
                    findcircle();
                    repaint();
                }
            }
        }else {
            points.add(new Point(e.getX(), e.getY()));
            updateShell();
            findcircle();
            repaint();
            Sound.playSound("data/click.wav").setVolume(1f);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
        dragging = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
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
        double a = (q.x - p.x) * (r.y - q.y) - (q.y - p.y) * (r.x - q.x);
        if (a == 0) return 0;
        if (a < 0) {
            return 1;
        } else {
            return 2;
        }
    }
    private void findcircle() {
        clearCollections();
        if (shell.size() < 2) return;

        processAllTriples();
        processAllPairs();
        filterAndSelectMinCircle();
    }

    private void clearCollections() {
        minСircle.clear();
        circles.clear();
        enclosingCircles.clear();
    }

    private void processAllTriples() {
        for (int i = 0; i < shell.size(); i++) {
            for (int j = i + 1; j < shell.size(); j++) {
                for (int k = j + 1; k < shell.size(); k++) {
                    processThreePoints(i, j, k);
                }
            }
        }
    }

    private void processThreePoints(int i, int j, int k) {
        Point p1 = shell.get(i);
        Point p2 = shell.get(j);
        Point p3 = shell.get(k);

        Circle circle = calculateCircleFromThreePoints(p1, p2, p3);
        if (circle != null) {
            circles.add(circle);
        }
    }

    private Circle calculateCircleFromThreePoints(Point p1, Point p2, Point p3) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x, y2 = p2.y;
        double x3 = p3.x, y3 = p3.y;

        double d = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
        if (d == 0) return null;

        double centerX = ((x1*x1 + y1*y1)*(y2-y3) + (x2*x2 + y2*y2)*(y3-y1) + (x3*x3 + y3*y3)*(y1-y2)) / d;
        double centerY = ((x1*x1 + y1*y1)*(x3-x2) + (x2*x2 + y2*y2)*(x1-x3) + (x3*x3 + y3*y3)*(x2-x1)) / d;
        double radius = Math.hypot(centerX - x1, centerY - y1);

        return new Circle(centerX, centerY, radius);
    }

    private void processAllPairs() {
        for (int i = 0; i < shell.size(); i++) {
            for (int j = i + 1; j < shell.size(); j++) {
                processTwoPoints(i, j);
            }
        }
    }

    private void processTwoPoints(int i, int j) {
        Point p1 = shell.get(i);
        Point p2 = shell.get(j);
        Circle circle = calculateCircleFromTwoPoints(p1, p2);
        circles.add(circle);
    }

    private Circle calculateCircleFromTwoPoints(Point p1, Point p2) {
        double centerX = (p1.x + p2.x) / 2;
        double centerY = (p1.y + p2.y) / 2;
        double radius = Math.hypot(p1.x - centerX, p1.y - centerY);
        return new Circle(centerX, centerY, radius);
    }

    private void filterAndSelectMinCircle() {
        filterValidCircles();
        if (!enclosingCircles.isEmpty()) {
            addMinCircleToWin();
        }
    }

    private void filterValidCircles() {
        for (Circle circle : circles) {
            if (allinside(shell, circle)) {
                enclosingCircles.add(new Circle(circle.x, circle.y, circle.r));
            }
        }
    }

    private void addMinCircleToWin() {
        int minIndex = findMinRadiusIndex();
        minСircle.add(new Circle(enclosingCircles.get(minIndex).x,
                enclosingCircles.get(minIndex).y,
                enclosingCircles.get(minIndex).r));
    }

    private int findMinRadiusIndex() {
        int minIndex = 0;
        for (int i = 1; i < enclosingCircles.size(); i++) {
            if (enclosingCircles.get(i).r < enclosingCircles.get(minIndex).r) {
                minIndex = i;
            }
        }
        return minIndex;
    }

    public static boolean allinside(ArrayList<Point> shell, Circle circle) {
        for (Point p : shell) {
            double distance = distance(p, circle);
            if (distance > circle.r + 0.000000001) {
                return false;
            }
        }
        return true;
    }
    private static double distance(Point p, Circle c) {
        return Math.sqrt((p.x-c.x)*(p.x-c.x)+(p.y-c.y)*(p.y-c.y));
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
        if (deletepoint == true){
            Circle deletecircle = new Circle(e.getX(), e.getY(), 5);
            Iterator<Point> iterator = points.iterator();
            while(iterator.hasNext()){
                Point point = iterator.next();
                if(distance(point, deletecircle) <= deletecircle.r){
                    Sound.playSound("data/delete.wav").setVolume(0.9f);
                    iterator.remove();
                    updateShell();
                    findcircle();
                    repaint();
                }
            }
        }else {
            if (dragging) {
                for (Point p : points) {
                    double deltaX = e.getX() - startX;
                    double deltaY = e.getY() - startY;
                    p.x += deltaX;
                    p.y += deltaY;
                }
                updateShell();
                findcircle();
                repaint();
                startX = e.getX();
                startY = e.getY();
            }
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
