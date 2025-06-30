import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.*;

import org.w3c.dom.events.MouseEvent;

//02/27/25 - Window boots up/runs faster, need to add JPanel Components
//03/01/25 - Changed implementation, added basic JPanel components
//04/10/25 - Changed implementation, reduced overhead, need to debug GUI
//GOOD PROGRESS - IMAGE SCROLL WORKS
//05/12/25 - INSANE PROGRESS - FUNCTIONING UI AND FAST DATA PROCESSING, HOLYSHIT SO HAPPY
//05/18/25 - GREAT PROGRESS - ALMOST COMPLETE READ/WRITE, NEED TO DEBUG ON-CLOSE EDGE CASE
public class PokeWildsModApp implements Runnable {
    private JFrame frame;
    private static DefaultTableModel dataModel;
    private static JTextArea entryStatView;
    private static JPanel entryDataView;
    protected static int currIndex;
    protected static int prevIndex;
    private static volatile boolean enterPressed;
    protected static String jarPath = "D:/Games/Pokémon Fan Games/pokewilds-windows64/"
                                        +"pokewilds-v0.8.11-windows-64/app/pokewilds.jar";
    protected static String modsPath = "D:/Games/Pokémon Fan Games/pokewilds-windows64/"
                                        +"pokewilds-v0.8.11-windows-64/mods/pokemon";
    protected static String movesPath = "pokemon/credited/evos_attacks.asm";
    protected static String statsPath = "pokemon/credited/base_stats/";
    //private static Color exitRed = new Color(255,0,0);
    private static Color skyBlue = new Color(165,165,255);
    //private static Color skyBlueA = new Color(165,165,255,150);
    private static Color limeGreen = new Color(220,220,200);
    private static Color white = new Color(255,255,255);

    public PokeWildsModApp(JFrame frame){
        this.frame = frame;
        enterPressed = false;
        PokeWildsModApp.currIndex = 0;
        PokeWildsModApp.prevIndex = 0;
    }

    public static void main(String[] args) {
        JarFileProcessor jp = new JarFileProcessor(jarPath, modsPath, movesPath, statsPath);
        jp.run();
        String viewEntry = jp.entries.get(0);
        entryStatView = new JTextArea();
        dataModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the third column
                return (column == 0 && row != 0);
            }
        };
        createAndShowGUI(jarPath, movesPath, viewEntry, jp, entryStatView, dataModel);
        /*KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(new KeyEventDispatcher() {        
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (PokeWildsModApp.class) {
                    switch (ke.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            if (ke.getKeyCode() == KeyEvent.VK_ENTER) 
                            { enterPressed = true; }
                            break;

                        case KeyEvent.KEY_RELEASED:
                            if (ke.getKeyCode() == KeyEvent.VK_ENTER) 
                            { enterPressed = false; }
                            break;
                    }
                    return false;
                }
            }
        });*/
    }

    @Override
    public void run(){
        //Display the window
        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setVisible(true);
        });
        try { Thread.sleep(1000); } catch (InterruptedException e) {
            System.out.println("Thread was interrupted.");
            return;
        } finally { System.out.println("ModEditor task completed."); }
    }

    public static void changeFont(Component component, Font font)
    {
        component.setFont(font);
        if (component instanceof Container)
        { for (Component child : ((Container)component).getComponents()) 
            { changeFont (child, font); }
        }
    }

    public static void changeBackground(Component component, Color color)
    {
        component.setBackground(color);
        if (component instanceof Container)
        { for (Component child : ((Container)component).getComponents()) 
            { changeBackground(child, color); }
        }
    }

    public static boolean enterPressed(){
        synchronized(PokeWildsModApp.class){ return enterPressed; }
    }

    public static void createAndShowGUI(String jarPath, String targetPath, 
                                        String viewEntry, JarFileProcessor jp,
                                        JTextArea entryStatView, DefaultTableModel dataModel){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double x = screenSize.getWidth();
        double y = screenSize.getHeight();

        //initialize JPanel elements, properties to automatically scale
        JFrame frame = new JFrame("ModEditor v0.1");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setUndecorated(false);
        frame.setPreferredSize(new Dimension(1024, 512));
        frame.setLocation((int)(x/2 - frame.getPreferredSize().getWidth()/2), 
                            (int)((y/2 - frame.getPreferredSize().getHeight()/2)));
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleClosing(frame, jp, entryStatView, dataModel);
            }
        });

        JTable learnsetTable = new JTable(dataModel);
        learnsetTable.setBackground(skyBlue);
        DefaultTableModel validMoveModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the third column
                return false;
            }
        };
        validMoveModel.addColumn("Name");
        validMoveModel.addColumn("Power");
        validMoveModel.addColumn("Type");
        for(String g: jp.validMoves.split("\n")){
            validMoveModel.addRow(new String[]{
                g.substring(0, g.indexOf(' ')),
                g.substring(g.indexOf(' ') + 1, g.lastIndexOf(' ')),
                g.substring(g.lastIndexOf(' ') + 1)
            });
        }
        JTable validMoveTable = new JTable(validMoveModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(validMoveTable.getModel());
        validMoveTable.setRowSorter(sorter);
        List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        validMoveTable.setAutoCreateRowSorter(true);

        JPanel mainPanel = new JPanel();
        eventListener frameDragListener = new eventListener(frame, mainPanel);
        mainPanel.addMouseListener(frameDragListener);
        mainPanel.addMouseMotionListener(frameDragListener);
        frame.add(mainPanel);

        mainPanel.setFocusable(true);
        try {
            JPanel namePanel = new JPanel();
            JPanel dataPanelA = new JPanel();
            JPanel dataPanelB = new JPanel();
            JPanel imagePanel = new JPanel();
            imagePanel.setBounds(new Rectangle(180,90));
            //DefaultListModel<String> model1 = new DefaultListModel<>();
            //JLabel nameLabel = new JLabel("PokéWilds Dex:", SwingConstants.CENTER);
            JLabel icon = new JLabel(JarFileProcessor.formatText(JarFileProcessor.targetPaths.
                                    get(0).getFileName().toString()), 
                                    new ImageIcon(jp.getImages().getFirst()), SwingConstants.CENTER);
            icon.setHorizontalTextPosition(JLabel.CENTER);
            icon.setVerticalTextPosition(JLabel.BOTTOM);
            icon.setName("spriteView");
            
            //JList<String> moveList = new JList<>(jp.validMoves.split("\n"));
            //JList<String> entryStats = new JList<>(model1);
            entryStatView.setMargin(new Insets(5,5,5,5));
            entryStatView.setBorder(new TitledBorder(BorderFactory
                                    .createLineBorder(white, 2), "Pokémon Entry"));
            entryStatView.setText(jp.entries.get(currIndex));
            entryStatView.setRows(10);
            entryStatView.setColumns(1);
            final JComboBox<String> comboBox = new JComboBox<String>(jp.getNames());
            comboBox.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) { ; }
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER){
                        setIndex(comboBox.getSelectedIndex());
                        viewUpdate(jp, learnsetTable, icon, jp.images, entryStatView, dataModel, 
                                    comboBox.getSelectedIndex());
                    }
                }
                @Override
                public void keyReleased(KeyEvent e) { ; }
            });
            AutoCompletion.enable(comboBox);

            viewUpdate(jp, learnsetTable, icon, jp.images, entryStatView, dataModel, 
                        currIndex);

            HashMap<String, JButton> btns = new HashMap<String, JButton>();
            btns.put("img_l", new JButton("<<"));
            btns.put("img_r", new JButton(">>"));
            btns.get("img_l").setSize(icon.getWidth()/2, icon.getHeight());
            btns.get("img_r").setSize(icon.getWidth()/2, icon.getHeight());
            btns.put("addMove", new JButton("<< Add"));
            btns.get("addMove").setEnabled(false);
            btns.put("delMove", new JButton("Delete"));
            btns.get("img_l").addActionListener(new clickHandler(frame, btns, icon, jp, 
                                                    currIndex, learnsetTable, 
                                                    entryStatView, validMoveTable, dataModel,
                                                    frameDragListener, comboBox));
            btns.get("img_r").addActionListener(new clickHandler(frame, btns, icon, jp, 
                                                    currIndex, learnsetTable, 
                                                    entryStatView, validMoveTable, dataModel, 
                                                    frameDragListener, comboBox));
            btns.get("addMove").addActionListener(new clickHandler(frame, btns, icon, jp, 
                                                    currIndex, learnsetTable, 
                                                    entryStatView, validMoveTable, dataModel, 
                                                    frameDragListener, comboBox));
            btns.get("delMove").addActionListener(new clickHandler(frame, btns, icon, jp, 
                                                    currIndex, learnsetTable, 
                                                    entryStatView, validMoveTable, dataModel, 
                                                    frameDragListener, comboBox));            
            /*moveList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) { 
                    if(learnsetTable.getSelectedRow() > 0)
                        btns.get("addMove").setEnabled(true); 
                }
            });*/

            validMoveTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if(learnsetTable.getSelectedRow() > 0 && validMoveTable.getSelectedRow() >= 0){
                        btns.get("addMove").setEnabled(true);
                    } else {
                        btns.get("addMove").setEnabled(false);
                    }
                }
            });
            //DO THIS - 3/28/25
            //Lot of rewrites, need to parse entries - 4/6/25
            JScrollPane learnScroll = new JScrollPane(learnsetTable);
            JScrollPane moveScroll = new JScrollPane(validMoveTable);
            /*JComboBox<String> type1Selector = new JComboBox<String>(JarFileProcessor
                                            .Type.values().toString().split(","));
            JComboBox<String> type2Selector = new JComboBox<String>(JarFileProcessor
                                            .Type.values().toString().split(","));
            setTypeBox(viewEntry, type1Selector);
            setTypeBox(viewEntry, type2Selector);
            JScrollPane type1 = new JScrollPane(type1Selector);
            JScrollPane type2 = new JScrollPane(type2Selector);
            */

            JButton searchBtn = new JButton("Search");
            JButton exitBtn = new JButton("X");
            exitBtn.setAlignmentX(SwingConstants.RIGHT);
            searchBtn.addActionListener(new clickHandler(frame, btns, icon, jp, currIndex, 
                                        learnsetTable, entryStatView, validMoveTable, dataModel, 
                                        frameDragListener, comboBox));
            exitBtn.addActionListener(new clickHandler(frame, btns, icon, jp, currIndex, 
                                        learnsetTable, entryStatView, validMoveTable, dataModel, 
                                        frameDragListener, comboBox));
            btns.put("search", searchBtn);
            btns.put("exit", exitBtn);

            JPanel inputPanel = new JPanel();
            JPanel searchLabelPanel = new JPanel();
            searchLabelPanel.add(new JLabel("Enter Pokémon Name Here:"));

            GroupLayout nLayout = new GroupLayout(namePanel);
            nLayout.setAutoCreateGaps(true);
            nLayout.setAutoCreateContainerGaps(true);
            nLayout.setHorizontalGroup(
                nLayout.createSequentialGroup()
                .addGroup(nLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    //.addComponent(nameLabel)
                    .addComponent(exitBtn)
                )
            );
            nLayout.setVerticalGroup(
                nLayout.createSequentialGroup()
                //.addGroup(nLayout.createParallelGroup(GroupLayout.Alignment.BASELINE))
                    //.addComponent(nameLabel)
                .addGroup(nLayout.createParallelGroup(GroupLayout.Alignment.BASELINE))
                    .addComponent(exitBtn)
            );
            
            GridBagLayout imgLayout = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 3;
            c.gridheight = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0,0,0,10);
            imagePanel.add(btns.get("img_l"));
            c.gridx = 1;
            c.insets = new Insets(5,5,5,5);
            icon.setBorder(new EmptyBorder(c.insets));
            c.insets = new Insets(0,10,0,10);
            imagePanel.add(icon);
            c.gridx = 2;
            imagePanel.add(btns.get("img_r"));
            c.insets = new Insets(10,10,10,10);
            imagePanel.setBorder(new EmptyBorder(c.insets));
            
            /*GridBagLayout dLayout = new GridBagLayout();
            GridBagConstraints d = new GridBagConstraints();
            d.fill = GridBagConstraints.BOTH;
            d.gridwidth = 5;
            d.gridheight = 2;
            d.gridx = 0;
            d.gridy = 0;
            d.weightx = 0.2;
            dataPanelA.add(imagePanel);
            d.gridx = 1;
            dataPanelA.add(entryStatView);
            d.gridx = 2;
            d.gridy = 1;
            d.anchor = GridBagConstraints.PAGE_START;
            dataPanelA.add(btns.get("addMove"));
            d.gridy = 2;
            d.anchor = GridBagConstraints.PAGE_END;
            dataPanelA.add(btns.get("delMove"));
            d.gridy = 10;
            d.gridx = 3;
            d.weightx = 0.5;
            d.fill = GridBagConstraints.BOTH;
            dataPanelA.add(learnScroll);
            d.gridy = 15;
            d.gridx = 4;
            d.weightx = 0.5;
            d.fill = GridBagConstraints.BOTH;
            dataPanelA.add(moveScroll);
            c.insets = new Insets(10,0,10,0);
            imagePanel.setBorder(new EmptyBorder(c.insets));
            */

            //((--HORIZONTAL GROUP EQUALS LEFT/RIGHT, VERTICAL GROUP EQUALS UP/DOWN--))
            GroupLayout dLayout = new GroupLayout(dataPanelA);
            dLayout.setAutoCreateGaps(false);
            dLayout.setAutoCreateContainerGaps(false);
            dLayout.setHorizontalGroup(
                dLayout.createSequentialGroup()
                    .addComponent(imagePanel, GroupLayout.PREFERRED_SIZE, 
                                GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                    .addComponent(entryStatView, GroupLayout.PREFERRED_SIZE, 
                                GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
            );
            dLayout.setVerticalGroup(
                dLayout.createSequentialGroup()
                    .addGroup(dLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(imagePanel)
                        .addComponent(entryStatView)
                    )
            );
            //((--HORIZONTAL GROUP EQUALS LEFT/RIGHT, VERTICAL GROUP EQUALS UP/DOWN--))
            GroupLayout dLayoutB = new GroupLayout(dataPanelB);
            dLayoutB.setAutoCreateGaps(true);
            dLayoutB.setAutoCreateContainerGaps(false);
            dLayoutB.setHorizontalGroup(
                dLayoutB.createSequentialGroup()
                    .addGroup(dLayoutB.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(dataPanelA)
                    .addGroup(dLayoutB.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addGroup(dLayoutB.createSequentialGroup()
                            .addComponent(learnScroll, GroupLayout.PREFERRED_SIZE, 
                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(3, 5, 5)
                                .addGroup(dLayoutB.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(btns.get("addMove"))
                                    .addComponent(btns.get("delMove"))
                                )
                            .addGap(3, 5, 5)
                                .addComponent(moveScroll, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                    )
                )
            );
            //((--HORIZONTAL GROUP EQUALS LEFT/RIGHT, VERTICAL GROUP EQUALS UP/DOWN--))
            dLayoutB.setVerticalGroup(
                dLayoutB.createSequentialGroup()
                    .addGroup(dLayoutB.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(dataPanelA)
                    )
                    .addGap(5,5,10)
                    .addGroup(dLayoutB.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(learnScroll)
                        .addGroup(dLayoutB.createSequentialGroup()
                            .addComponent(btns.get("addMove"))
                            .addComponent(btns.get("delMove"))
                        )
                        .addComponent(moveScroll)
                    )
            );
            GridBagLayout inLayout = new GridBagLayout();
            GridBagConstraints r = new GridBagConstraints();
            r.fill = GridBagConstraints.HORIZONTAL;
            r.gridwidth = 3;
            r.gridheight = 1;
            r.gridx = 0;
            r.gridy = 0;
            r.insets = new Insets(0,10,0,10);
            inputPanel.add(searchLabelPanel);
            r.gridx = 1;
            inputPanel.add(comboBox);
            r.gridx = 2;
            inputPanel.add(searchBtn);
            inputPanel.setBorder(new EmptyBorder(r.insets));
            
            GroupLayout layout = new GroupLayout(mainPanel);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(namePanel)
                        .addComponent(dataPanelB)
                        .addComponent(inputPanel)
                    )
            );
            layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(namePanel))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(dataPanelB))
                        .addGap(5,5,10)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(inputPanel)
                )
            );
            mainPanel.setLayout(layout);
            namePanel.setLayout(nLayout);
            imagePanel.setLayout(imgLayout);
            dataPanelA.setLayout(dLayout);
            dataPanelB.setLayout(dLayoutB);
            inputPanel.setLayout(inLayout);

            layout.maximumLayoutSize(mainPanel);
            nLayout.maximumLayoutSize(namePanel);
            imgLayout.maximumLayoutSize(imagePanel);
            dLayout.maximumLayoutSize(dataPanelA);
            dLayoutB.maximumLayoutSize(dataPanelB);
            inLayout.maximumLayoutSize(inputPanel);

            changeFont(namePanel, new Font("Bahnschrift", Font.BOLD, 20));
            changeFont(mainPanel, new Font("Bahnschrift", Font.PLAIN, 10));
            changeFont(inputPanel, new Font("Bahnschrift", Font.BOLD, 12));
            changeFont(icon, new Font("Bahnschrift", Font.BOLD, 12));
            changeBackground(frame, skyBlue);
            changeBackground(exitBtn, white);
            changeBackground(entryStatView, limeGreen);
            changeBackground(learnScroll, limeGreen);
            changeBackground(moveScroll, limeGreen);
            changeBackground(validMoveTable, limeGreen);
            changeBackground(btns.get("img_l"), limeGreen);
            changeBackground(btns.get("img_r"), limeGreen);
            changeBackground(btns.get("addMove"), limeGreen);
            changeBackground(btns.get("delMove"), limeGreen);
            changeBackground(comboBox, limeGreen);
            changeBackground(searchBtn, limeGreen);

        } catch (Exception e) { e.printStackTrace(); }

        PokeWildsModApp asyncJFrame = new PokeWildsModApp(frame);
        Thread thread = new Thread(asyncJFrame);
        thread.start();
        
        try { thread.join(); } 
        catch (InterruptedException e) { System.out.println("Main thread was interrupted!"); } 
        finally { System.out.println("Main thread resumed."); }
    }

    private static void setTypeBox(String viewEntry, JComboBox<String> selector){
        String matchType = null;
        if(selector.getName().equals("type1Selector")){
            for(JarFileProcessor.Type t : JarFileProcessor.Type.values()){
            matchType = viewEntry.substring(0, 
                viewEntry.indexOf(System.lineSeparator()))
                .equalsIgnoreCase(t.name()) ? t.name() : null;
            }
        } else {
            for(JarFileProcessor.Type t : JarFileProcessor.Type.values()){
            matchType = viewEntry.substring(viewEntry.indexOf(",") + 1,
                viewEntry.indexOf(System.lineSeparator()))
                .equalsIgnoreCase(t.name()) ? t.name() : null;
            }
        }
        if(matchType != null){ selector.setSelectedItem(matchType); }
    }
    public static void setIndex(int value) { currIndex = value; }

    public static void jarUpdate(JarFileProcessor jp, JTextArea entryStatView, 
                                DefaultTableModel dataModel, int idx, int row)
    {
        // default - save entry 0
        String temp = "";
        // 05/18/25 - ONLY OVERWRITE ENTRIES IF CHANGE OCCURRED
        // CURRENTLY TRIGGERS WHEN CLOSE CLICKED ON CURRINDEX W/ NO CHANGES
        if(prevIndex != currIndex){
            jp.entries.put(idx, entryStatView.getText());
            for (int z = row; z < dataModel.getRowCount(); z++) {
                temp += (dataModel.getDataVector().elementAt(z)).get(0).toString()
                        .concat(" " + (dataModel.getDataVector().elementAt(z)).get(1).toString());
                if (dataModel.getDataVector().elementAt(z).get(2) != null) {
                    temp += " " + (dataModel.getDataVector().elementAt(z)).get(2).toString()
                            .concat(System.lineSeparator());
                } else
                    temp += System.lineSeparator();
            }
            jp.entryMoves.put(idx, temp);
            if (!jp.statDirs.contains(JarFileProcessor.targetPaths.get(idx))) {
                jp.statDirs.add(JarFileProcessor.targetPaths.get(idx));
            }
            if (!jp.moveDirs.contains(JarFileProcessor.targetPaths.get(idx))) {
                jp.moveDirs.add(JarFileProcessor.targetPaths.get(idx));
            }
        }       
    }

    public static void viewUpdate(JarFileProcessor jp, JTable learnset, JLabel i, 
                            HashMap<Integer,BufferedImage> images, JTextArea entryStatView, 
                            DefaultTableModel dataModel, int idx) {
        String[] columnNames = {"Level", "Condition I", "Condition II"};
        int row = 0;

        jarUpdate(jp, entryStatView, dataModel, prevIndex, row);
                
        i.setIcon(new ImageIcon(JarFileProcessor.resize(images.get(idx), 1.5, 1.5)));
        i.setText(JarFileProcessor.formatText(JarFileProcessor.targetPaths.
                get(idx).getFileName().toString()));
        if(dataModel.getRowCount() <= 0){
            dataModel.addColumn(columnNames[0]);
            dataModel.addColumn(columnNames[1]);
            dataModel.addColumn(columnNames[2]);
        }
        //viewUpdate called on init and when button pressed
        // USING TABLEMODEL FOR LEARNSET, ADD TO JSCROLLPANE
        dataModel.setRowCount(0);
        for (String g : jp.showData(jp.entryMoves, idx)) {
            if(g.matches("([\\D ]+|\\D+[\\d ]+\\D+)")){ 
                dataModel.insertRow(row, new String[] 
                {   g.substring(0, g.indexOf(' ')), 
                    g.substring(g.indexOf(' ') + 1, g.lastIndexOf(' ')),
                    g.substring(g.lastIndexOf(' ') + 1)}); 
                    dataModel.isCellEditable(row, row);
                }
            else
                dataModel.insertRow(row, new String[] 
                {   g.substring(0, g.indexOf(' ')), 
                    g.substring(g.indexOf(' ') + 1) });
            row++;
        }
        learnset.setModel(dataModel);
        entryStatView.setText(null);
        entryStatView.append(jp.entries.get(idx));
        /*for (String h : jp.showData(jp.entries, idx)) { model1.addElement(h); }
        entryStats.setModel(model1);*/
    }

    public static class eventListener extends MouseAdapter {
        private final JFrame frame;
        //private final JPanel panel;
        private Point mouseDownCompCoords;

        public eventListener(JFrame frame, JPanel panel) {
            this.frame = frame;
            //this.panel = panel;
            this.mouseDownCompCoords = null;
        }
        public void mouseReleased(MouseEvent e) {
            mouseDownCompCoords = null;
        }
        public void mousePressed(MouseEvent e) {
            mouseDownCompCoords = new Point(e.getScreenX(), e.getScreenY());
        }
        /*
        public void mouseDragged(MouseEvent e) {
            this.mouseDownCompCoords = null;
        }*/
        public void mouseDragged(MouseEvent e) {
            Point currCoords = new Point(e.getScreenX(), e.getScreenY());
            //translate window left, right, up, down
            if(currCoords.x < mouseDownCompCoords.x)
                frame.setLocation((int)(frame.getLocation().getX() 
                - (mouseDownCompCoords.x - currCoords.x)), 
                (int)(frame.getLocation().getY()));
            if(currCoords.x > mouseDownCompCoords.x)
                frame.setLocation((int)(frame.getLocation().getX() 
                + (currCoords.x - mouseDownCompCoords.x)),
                (int)(frame.getLocation().getY()));
            if(currCoords.y < mouseDownCompCoords.y)
                frame.setLocation((int)(frame.getLocation().getX()), 
                (int)(frame.getLocation().getY() - (mouseDownCompCoords.y - currCoords.y)));
            if(currCoords.y > mouseDownCompCoords.y)
                frame.setLocation((int)(frame.getLocation().getX()), 
                (int)(frame.getLocation().getY() + (currCoords.y - mouseDownCompCoords.y)));
        }
    }

    private static void handleClosing(JFrame frame, JarFileProcessor jp,
                                    JTextArea statView, DefaultTableModel dataModel) {
        if (hasUnsaveData()) {
            int answer = showWarningMessage(frame);
            switch (answer) {
                case JOptionPane.YES_OPTION:
                    System.out.println("Save and Quit");
                    jarUpdate(jp, statView, dataModel, currIndex, 0);
                    JarFileProcessor.saveEdits(statView.getText(), dataModel.getDataVector().toString(), 
                                            jp.entries, jp.entryMoves, modsPath, jp.statDirs, 
                                            jp.moveDirs, prevIndex);
                    frame.dispose();
                    break;
                     
                case JOptionPane.NO_OPTION:
                    System.out.println("Don't Save and Quit");
                    frame.dispose();
                    break;
                     
                case JOptionPane.CANCEL_OPTION:
                    System.out.println("Don't Quit");
                    break;
            }
        } else { frame.dispose(); }      
    }

    private static int showWarningMessage(JFrame frame) {
        String[] buttonLabels = new String[] {"Yes", "No", "Cancel"};
        String defaultOption = buttonLabels[0];
        Icon icon = null;
         
        return JOptionPane.showOptionDialog(frame,
                "Save Changes Before Exiting?",
                "Warning!",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                icon,
                buttonLabels,
                defaultOption);    
    }
 
    private static boolean hasUnsaveData() {
        // checks if there's still something unsaved
        // this method always return true for demo purpose
        return true;
    }

    public static class clickHandler implements ActionListener {
        private JFrame frame;
        private HashMap<String, JButton> button;
        private DefaultTableModel dataModel;
        private JLabel i;
        private HashMap<Integer,BufferedImage> images;
        private JarFileProcessor jp;
        private JTable learnset;
        private JTable moveList;
        private JTextArea statView;
        //private eventListener listener;
        private JComboBox<String> comboBox;

        public clickHandler(JFrame frame, HashMap<String, JButton> buttons, JLabel i, 
                            JarFileProcessor jp, int currIndex, JTable entryLearnset, 
                            JTextArea entryStatView, JTable moveList, 
                            DefaultTableModel model, eventListener eventListener, 
                            JComboBox<String> comboBox){
            this.frame = frame;
            this.button = buttons;
            this.dataModel = model;
            this.i = i;
            this.images = jp.images;
            this.jp = jp;
            this.learnset = entryLearnset;
            this.statView = entryStatView;
            this.moveList = moveList;
            this.comboBox = comboBox;
            //this.b = jp.getImages();
            //this.listener = eventListener;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();

            if(source.equals(button.get("img_l"))){
                if(currIndex == 0)
                {   
                    prevIndex = currIndex;
                    setIndex(jp.entries.size()-1);
                    viewUpdate(jp, learnset, i, images, statView, dataModel, 
                                currIndex);
                }
                else {
                    prevIndex = currIndex;
                    setIndex(--currIndex);
                    viewUpdate(jp, learnset, i, images, statView, dataModel, 
                                currIndex); 
                }
            }
            if(source.equals(button.get("img_r"))){
                if(currIndex == jp.entries.size()-1)
                { 
                    prevIndex = currIndex;
                    setIndex(0);
                    viewUpdate(jp, learnset, i, images, statView, dataModel, 
                                currIndex);
                }
                else { 
                    prevIndex = currIndex;
                    setIndex(++currIndex);
                    viewUpdate(jp, learnset, i, images, statView, dataModel, 
                                currIndex);
                }
            }
            if(source.equals(button.get("addMove"))){
                dataModel.insertRow(learnset.getSelectedRow() + 1,
                    new String[] { (String)learnset.getValueAt(learnset.getSelectedRow(), 0), 
                                    (String)moveList.getValueAt(moveList.getSelectedRow(), 0) 
                                });
            }
            //##### 05/07/25 RESUME HERE, RECONFIGURED UI AND CHANGED LEARNSET DISPLAY IMPL
            if(source.equals(button.get("delMove"))){
                dataModel.removeRow(learnset.getSelectedRow());
            }
            if(source.equals(button.get("search"))){
                prevIndex = currIndex;
                setIndex(comboBox.getSelectedIndex());
                viewUpdate(jp, learnset, i, images, statView, dataModel, 
                            currIndex);
            }
            if(source.equals(button.get("exit"))){
                handleClosing(frame, jp, statView, dataModel);
            }
        }
    }
    /* This work is hereby released into the Public Domain.
    * To view a copy of the public domain dedication, visit
    * http://creativecommons.org/licenses/publicdomain/
    */
    public static class AutoCompletion extends PlainDocument {
        JComboBox<String> comboBox;
        @SuppressWarnings("rawtypes")
        ComboBoxModel model;
        JTextComponent editor;
        // flag to indicate if setSelectedItem has been called
        // subsequent calls to remove/insertString should be ignored
        boolean selecting=false;
        boolean hidePopupOnFocusLoss;
        boolean hitBackspace=false;
        boolean hitBackspaceOnSelection;

        KeyListener editorKeyListener;
        FocusListener editorFocusListener;

        public AutoCompletion(final JComboBox<String> comboBox) {
            this.comboBox = comboBox;
            model = comboBox.getModel();
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!selecting) highlightCompletedText(0);
                }
            });
            comboBox.addPropertyChangeListener(new PropertyChangeListener() {
                @SuppressWarnings("rawtypes")
                public void propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("editor")) 
                        configureEditor((ComboBoxEditor) e.getNewValue());
                    if (e.getPropertyName().equals("model")) 
                        model = (ComboBoxModel) e.getNewValue();
                }
            });
            editorKeyListener = new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (comboBox.isDisplayable()) comboBox.setPopupVisible(true);
                    hitBackspace=false;
                    switch (e.getKeyCode()) {
                        // determine if the pressed key is backspace (needed by the remove method)
                        case KeyEvent.VK_BACK_SPACE : hitBackspace=true;
                        hitBackspaceOnSelection=editor.getSelectionStart()!=editor.getSelectionEnd();
                        break;
                        // ignore delete key
                        case KeyEvent.VK_DELETE : e.consume();
                        comboBox.getToolkit().beep();
                        break;
                    }
                }
            };
            // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
            hidePopupOnFocusLoss=System.getProperty("java.version").startsWith("1.5");
            // Highlight whole text when gaining focus
            editorFocusListener = new FocusAdapter() {
                public void focusGained(FocusEvent e) { highlightCompletedText(0); }
                // Workaround for Bug 5100422 - Hide Popup on focus loss
                public void focusLost(FocusEvent e) { if (hidePopupOnFocusLoss) comboBox.setPopupVisible(false); }
            };
            configureEditor(comboBox.getEditor());
            // Handle initially selected object
            Object selected = comboBox.getSelectedItem();
            if (selected!=null) setText(selected.toString());
            highlightCompletedText(0);
        }

        public static void enable(JComboBox<String> comboBox) {
            // has to be editable
            comboBox.setEditable(true);
            // change the editor's document
            new AutoCompletion(comboBox);
        }

        void configureEditor(ComboBoxEditor newEditor) {
            if (editor != null) {
                editor.removeKeyListener(editorKeyListener);
                editor.removeFocusListener(editorFocusListener);
            }

            if (newEditor != null) {
                editor = (JTextComponent) newEditor.getEditorComponent();
                editor.addKeyListener(editorKeyListener);
                editor.addFocusListener(editorFocusListener);
                editor.setDocument(this);
            }
        }

        public void remove(int offs, int len) throws BadLocationException {
            // return immediately when selecting an item
            if (selecting) return;
            if (hitBackspace) {
                // user hit backspace => move the selection backwards
                // old item keeps being selected
                if (offs>0) {
                    if (hitBackspaceOnSelection) offs--;
                } else {
                    // User hit backspace with the cursor positioned on the start => beep
                    // when available use: UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
                    comboBox.getToolkit().beep();
                }
                highlightCompletedText(offs);
            } else { super.remove(offs, len); }
        }

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            // return immediately when selecting an item
            if (selecting) return;
            // insert the string into the document
            super.insertString(offs, str, a);
            // lookup and select a matching item
            Object item = lookupItem(getText(0, getLength()));
            if (item != null) {
                setSelectedItem(item);
            } else {
                // keep old item selected if there is no match
                item = comboBox.getSelectedItem();
                // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
                offs = offs-str.length();
                // provide feedback to the user that his input has been received but can not be accepted
                comboBox.getToolkit().beep(); // when available use: UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
            }
            setText(item.toString());
            // select the completed part
            highlightCompletedText(offs+str.length());
        }

        private void setText(String text) {
            try {
                // remove all text and insert the completed string
                super.remove(0, getLength());
                super.insertString(0, text, null);
            } catch (BadLocationException e) { throw new RuntimeException(e.toString()); }
        }

        private void highlightCompletedText(int start) {
            editor.setCaretPosition(getLength());
            editor.moveCaretPosition(start);
        }

        private void setSelectedItem(Object item) {
            selecting = true;
            model.setSelectedItem(item);
            selecting = false;
        }

        private Object lookupItem(String pattern) {
            Object selectedItem = model.getSelectedItem();
            // only search for a different item if the currently selected does not match
            if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
                return selectedItem;
            } else {
                // iterate over all items
                for (int i=0, n=model.getSize(); i < n; i++) {
                    Object currentItem = model.getElementAt(i);
                    // current item starts with the pattern?
                    if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), pattern)) {
                        return currentItem;
                    }
                }
            }
            // no item starts with the pattern => return null
            return null;
        }
        // checks if str1 starts with str2 - ignores case
        private boolean startsWithIgnoreCase(String str1, String str2) {
            return str1.toUpperCase().startsWith(str2.toUpperCase());
        }
    }
}


