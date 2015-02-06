package com.adamsresearch.jdbcquerytool;

/**
 * Created by wma on 2/5/15.
 *
 * Note: this is what I call an "accreted utility". It is >not< an example of stellar programming!
 */
import java.sql.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class Query extends JPanel {

  protected final int CASE_SENSITIVE_SCHEMA_NAME_STRATEGY = 0;
  protected final int CASE_INSENSITIVE_SCHEMA_NAME_STRATEGY = 1;
  protected final int NULL_SCHEMA_NAME_STRATEGY = 2;
  protected Query thisPanel;
  protected ResourceBundle bundle;
  protected JLabel connectionLabel;
  protected JComboBox connectionsCombo;
  protected JButton newConnectionButton;
  protected JButton editConnectionButton;
  protected JButton deleteConnectionButton;
  protected JButton cancelQueryButton;
  protected JButton connectButton;
  protected JButton disconnectButton;
  protected JLabel metadataLabel;
  protected JLabel queryLabel;
  protected JTextArea queryArea;
  protected JScrollPane queryScroll;
  protected JButton queryButton;
  protected JScrollPane outputScroll;
  protected JTextArea outputArea;
  protected TreeModel metadataTreeModel = null;
  protected JTree metadataTree;
  protected JScrollPane metadataScroll;
  protected JSplitPane splitPane;
  protected Connection conn;
  protected Statement stmt;
  protected DatabaseMetaData dbMetaData;
  protected JSplitPane metadataSplitPane;
  protected QueryExecutor queryExecutor = null;
  protected JProgressBar progressBar;
  protected int numberOfTables;
  protected int currentTableNumber;
  protected int schemaNameStrategy;
  
  public static void main(String args[]) {
    final Query query = new Query();
    ResourceBundle bundle = ResourceBundle.getBundle(Query.class.getName());
    JFrame mainFrame = new JFrame(bundle.getString("appLabel"));
    mainFrame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        query.closeConnection();
        System.exit(0);
      }
    });
    mainFrame.getContentPane().add(query);
    mainFrame.setSize(1000, 800);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension toolSize = mainFrame.getSize();
    int xLoc = (screenSize.width - toolSize.width)/2;
    int yLoc = (screenSize.height - toolSize.height)/2;
    mainFrame.setLocation(xLoc, yLoc);
    mainFrame.setVisible(true);
    query.setInitialDividerLocations();
  }

  public Query() {
    
    thisPanel = this;
    bundle = ResourceBundle.getBundle(Query.class.getName());
    layOutTool();
    initializeTool();
    addListeners();
    
  }
  
  protected void layOutTool() {
    connectionLabel = new JLabel(bundle.getString("connectionLabel"));
    connectionsCombo = new JComboBox();
    connectionsCombo.setPrototypeDisplayValue("Sample connection identifier");
    newConnectionButton = new JButton(bundle.getString("newConnectionButton"));
    editConnectionButton = new JButton(bundle.getString("editConnectionButton"));
    deleteConnectionButton = new JButton(bundle.getString("deleteConnectionButton"));
    metadataLabel = new JLabel(bundle.getString("metadataLabel"));
    queryLabel = new JLabel(bundle.getString("queryLabel"));
    queryArea = new JTextArea();
    queryArea.setLineWrap(true);
    queryArea.setWrapStyleWord(true);
    queryArea.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
    queryScroll = new JScrollPane(queryArea,
                                 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    progressBar = new JProgressBar();
    progressBar.setStringPainted(true);
    progressBar.setString(null);
                                 
    // separate panel to hold query label and queries
    JPanel queryPanel = new JPanel();
    GridBagLayout qpGbl = new GridBagLayout();
    GridBagConstraints qpGbc = new GridBagConstraints();
    queryPanel.setLayout(qpGbl);
    
    qpGbc.gridx = 0; qpGbc.gridy = 0; qpGbc.gridheight = 1; qpGbc.gridwidth = 1;
    qpGbc.weightx = 0.0; qpGbc.weighty = 0.0; qpGbc.fill = GridBagConstraints.NONE;
    qpGbc.anchor = GridBagConstraints.WEST;
    queryPanel.add(queryLabel, qpGbc);

    qpGbc.gridx = 0; qpGbc.gridy = 1; qpGbc.gridheight = 3; qpGbc.gridwidth = 6;
    qpGbc.weightx = 1.0; qpGbc.weighty = 1.0; qpGbc.fill = GridBagConstraints.BOTH;
    qpGbc.anchor = GridBagConstraints.WEST;
    queryPanel.add(queryScroll, qpGbc);
    
    cancelQueryButton = new JButton(bundle.getString("cancelQueryButton"));
    cancelQueryButton.setEnabled(false);
    connectButton = new JButton(bundle.getString("connectButton"));
    disconnectButton = new JButton(bundle.getString("disconnectButton"));
    queryButton = new JButton(bundle.getString("executeButton"));
    outputArea = new JTextArea();
    outputArea.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
    outputScroll = new JScrollPane(outputArea,
                                 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, queryPanel, outputScroll);
    splitPane.setOneTouchExpandable(true);
    metadataTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Metadata"));
    metadataTree = new JTree(metadataTreeModel);
    metadataScroll = new JScrollPane(metadataTree,
                                     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    JPanel metadataPanel = new JPanel();
    GridBagLayout mpGbl = new GridBagLayout();
    GridBagConstraints mpGbc = new GridBagConstraints();
    metadataPanel.setLayout(mpGbl);
    
    mpGbc.gridx = 0; mpGbc.gridy = 0; mpGbc.gridheight = 1; mpGbc.gridwidth = 1;
    mpGbc.weightx = 0.0; mpGbc.weighty = 0.0; mpGbc.fill = GridBagConstraints.NONE;
    mpGbc.anchor = GridBagConstraints.WEST;
    mpGbc.insets = new Insets(2,2,2,2);
    metadataPanel.add(metadataLabel, mpGbc);
    
    mpGbc.gridx = 1; mpGbc.gridy = 0; mpGbc.gridheight = 1; mpGbc.gridwidth = 1;
    mpGbc.weightx = 1.0; mpGbc.weighty = 0.0; mpGbc.fill = GridBagConstraints.HORIZONTAL;
    mpGbc.anchor = GridBagConstraints.WEST;
    mpGbc.insets = new Insets(2,10,2,10);
    metadataPanel.add(progressBar, mpGbc);
    progressBar.setVisible(false);
    
    mpGbc.gridx = 0; mpGbc.gridy = 1; mpGbc.gridheight = 12; mpGbc.gridwidth = 3;
    mpGbc.weightx = 1.0; mpGbc.weighty = 1.0; mpGbc.fill = GridBagConstraints.BOTH;
    mpGbc.anchor = GridBagConstraints.WEST;
    mpGbc.insets = new Insets(2,2,2,2);
    metadataPanel.add(metadataScroll, mpGbc);
    
	  metadataSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, metadataPanel, splitPane);
    metadataSplitPane.setOneTouchExpandable(true);
    
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    thisPanel.setLayout(gbl);
    
    gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(connectionLabel, gbc);
    
    gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(connectionsCombo, gbc);
    
    gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(newConnectionButton, gbc);
    
    gbc.gridx = 3; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(editConnectionButton, gbc);
    
    gbc.gridx = 4; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(deleteConnectionButton, gbc);
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout());
    buttonPanel.add(cancelQueryButton);
    buttonPanel.add(queryButton);
    buttonPanel.add(connectButton);
    buttonPanel.add(disconnectButton);

    gbc.gridx = 3; gbc.gridy = 1; gbc.gridheight = 1; gbc.gridwidth = 3;
    gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
    thisPanel.add(buttonPanel, gbc);

    gbc.gridx = 0; gbc.gridy = 2; gbc.gridheight = 10; gbc.gridwidth = 6;
    gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;
    thisPanel.add(metadataSplitPane, gbc);
    
    queryButton.setEnabled(false);
    connectButton.setEnabled(true);
    disconnectButton.setEnabled(false);
  }
  
  protected void initializeTool() {
    Properties connectionProps = new Properties();
    try {
      connectionProps.load(this.getClass().getClassLoader().getResourceAsStream("com/adamsresearch/jdbcquerytool/connections.config"));
    } catch (FileNotFoundException fnfe) {
      System.out.println("Unable to find property file 'connections.config'; will create new one...");
    } catch (IOException ioe) {
      System.out.println("Unable to find property file 'connections.config'; will create new one...");
    }
    connectionsCombo.removeAllItems();
    for (Enumeration propNames = connectionProps.propertyNames(); propNames.hasMoreElements();) {
      String connName = (String)propNames.nextElement();
      String connInfo = connectionProps.getProperty(connName);
      DatabaseConnection dc = new DatabaseConnection(connName, connInfo);
      connectionsCombo.addItem(dc);
    }
    this.loadQueries();
    thisPanel.invalidate();
    thisPanel.validate();
    thisPanel.repaint();
  }
  
  public void setInitialDividerLocations()
  {
      splitPane.setDividerLocation(0.4);
      metadataSplitPane.setDividerLocation(0.0);
  }
  
  protected void addListeners() {
    
    connectionsCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
                  boolean cancel = false;
          if (disconnectButton.isEnabled())
          {
            Object[] options = {bundle.getString("ok"), bundle.getString("cancel")};
            String warningLabel = bundle.getString("closeConnectionLabel");
            String warning = bundle.getString("closeConnectionWarning");
            int selectedOption = JOptionPane.showOptionDialog(null, warning, warningLabel, 
                                 JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                 null, options, options[0]);
            if (selectedOption == 0) {
              thisPanel.closeConnection();
            }
            else {
              cancel = true;
            }
          }

          if (!cancel) {
          }
      }
    });
    
    connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        String statusString = "";
        DatabaseConnection dConn = (DatabaseConnection)(connectionsCombo.getSelectedItem());
        Properties dbProps = dConn.getDbProperties();
        try {
          Class.forName(dConn.getDriverClassName());
          // choices based on how much input the user gave:
          if ( ((dConn.getUsername() != null) && (!dConn.getUsername().equals(""))) &&
                ((dConn.getPassword() != null) && (!dConn.getPassword().equals(""))) ) {
            // assume user wants username/password login:
            conn = DriverManager.getConnection(dConn.getDatabaseUrl(), dConn.getUsername(), dConn.getPassword());
          }
          // unfortunately, MySQL had to throw their own twist into things:
          else if ( ((dConn.getUsername() != null) && (!dConn.getUsername().equals(""))) &&
                    ((dConn.getPassword() == null) || (dConn.getPassword().trim().length() == 0)) &&
                    dConn.getDriverClassName().equals("com.mysql.jdbc.Driver")) {
            System.out.println("Special connection case for MySQL:");
 /*           String connectionString = dConn.getDatabaseUrl() + "?user=" + dConn.getUsername() +
                                      "&password=" + dConn.getPassword();
              System.out.println("Connection string: '" + connectionString + "'");*/
            conn = DriverManager.getConnection(dConn.getDatabaseUrl(), dConn.getUsername(), "");
          }
          else if (dbProps.size() > 0) {
            // assume user wants to log in by passing in a Properties object:
            conn = DriverManager.getConnection(dConn.getDatabaseUrl(), dbProps);
          }
          else {
            // no username/password and no properties string - simple login:
            conn = DriverManager.getConnection(dConn.getDatabaseUrl());
          }
          stmt = conn.createStatement();
          // retrieve the database metadata in a separate thread; it will use
          // SwingUtilities.invokeLater() to update the metadata panel when it
          // is done.
          progressBar.setVisible(true);
          progressBar.setIndeterminate(true);
          metadataSplitPane.setDividerLocation(0.3);
          Runnable metadataThread = new Runnable() {
            public void run()
            {
              thisPanel.setDatabaseMetadata();
            }
          };
          new Thread(metadataThread).start();
          // these should always be disabled.  it's possible to have text selected
          // from a previous connection, thus enabling the query button, but that
          // is confusing because having lost focus, the selection in the window
          // is no longer visible:
          queryButton.setEnabled(false);
          connectButton.setEnabled(false);
          disconnectButton.setEnabled(true);
        } catch (ClassNotFoundException cnfe) {
          statusString += "Could not load driver '" + dConn.getDriverClassName() + "'\n";
        } catch (SQLException sqle) {
          statusString += "SQLException getting connection: '" + sqle.getMessage() + "'\n";
          SQLException sqle2 = sqle.getNextException();
          if (sqle2 != null) {
            statusString += "Embedded exception: '" + sqle2.getMessage() + "'\n";
          }
          statusString += "  -> database driver class name: '" + dConn.getDriverClassName() + "'\n";
          statusString += "  -> database URL: '" + dConn.getDatabaseUrl() + "'\n";
          statusString += "  -> database properties:\n";
          dbProps = dConn.getDbProperties();
          for (Enumeration propNames = dbProps.propertyNames(); propNames.hasMoreElements();) {
            Object nextKey = propNames.nextElement();
            statusString += "     - " + nextKey + "=" + dbProps.getProperty((String)nextKey) + "\n";
          }
        }
        if (statusString != "") {
          outputArea.setText(statusString);
        }
        thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    });
    
    queryButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        // store all queries now:
        thisPanel.storeQueries();
        String outputString = "";
        String queryString = queryArea.getSelectedText();
        try {
          if (queryString.toLowerCase().startsWith("select")) {
            queryExecutor = new QueryExecutor("name", stmt, queryString, thisPanel);
            queryExecutor.start();
            cancelQueryButton.setEnabled(true);
          }
          else {
            int numRows = stmt.executeUpdate(queryString);
            outputString += "(" + numRows + " row(s) ";
            if (queryString.toLowerCase().startsWith("insert")) {
              outputString += "inserted)\n";
            }
            else if (queryString.toLowerCase().startsWith("update")) {
              outputString += "updated)\n";
            }
            else if (queryString.toLowerCase().startsWith("delete")) {
              outputString += "deleted)\n";
            }
            outputArea.setText(outputString);
            thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
          }
        } catch (SQLException sqle) {
          outputString = "SQLException executing query: '" + sqle.getMessage() + "'";
        }
        queryArea.setSelectionEnd(queryArea.getSelectionStart());
        queryButton.setEnabled(false);
      }
    });
    
    disconnectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        thisPanel.closeConnection();
        thisPanel.clearDatabaseMetadata();
        thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    });
    
    cancelQueryButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        queryExecutor.cancelQuery();
      }
    });
    
    newConnectionButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        boolean cancel = false;
        if (disconnectButton.isEnabled())
        {
          Object[] options = {bundle.getString("ok"), bundle.getString("cancel")};
          String warningLabel = bundle.getString("closeConnectionLabel");
          String warning = bundle.getString("closeConnectionWarning");
          int selectedOption = JOptionPane.showOptionDialog(null, warning, warningLabel, 
                               JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                               null, options, options[0]);
          if (selectedOption == 0) {
            thisPanel.closeConnection();
          }
          else {
            cancel = true;
          }
        }

        if (!cancel) {
          ConnectionDialog cd = new ConnectionDialog(bundle, new DatabaseConnection());
          cd.setLocationRelativeTo(null);
          cd.setVisible(true);
          if (cd.selectedOK()) {
            DatabaseConnection newConn = cd.getDatabaseConnection();
            connectionsCombo.addItem(newConn);
            connectionsCombo.setSelectedItem(newConn);
            thisPanel.storeConnections();
          }
        }
      }
    });
    editConnectionButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (connectionsCombo.getSelectedIndex() > -1) {
          boolean cancel = false;
          if (disconnectButton.isEnabled())
          {
            Object[] options = {bundle.getString("ok"), bundle.getString("cancel")};
            String warningLabel = bundle.getString("closeConnectionLabel");
            String warning = bundle.getString("closeConnectionWarning");
            int selectedOption = JOptionPane.showOptionDialog(null, warning, warningLabel, 
                                 JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                 null, options, options[0]);
            if (selectedOption == 0) {
              thisPanel.closeConnection();
            }
            else {
              cancel = true;
            }
          }

          if (!cancel) {
            ConnectionDialog cd = new ConnectionDialog(bundle, (DatabaseConnection)(connectionsCombo.getSelectedItem()));
            cd.setLocationRelativeTo(null);
            cd.setVisible(true);
            if (cd.selectedOK()) {
              connectionsCombo.setSelectedItem(cd.getDatabaseConnection());
              thisPanel.storeConnections();
            }
          }
        }
      }
    });
    deleteConnectionButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int selIndex = connectionsCombo.getSelectedIndex();
        if (selIndex > -1) {
          boolean cancel = false;
          // first confirm if user really meant it - I've been there!
          Object[] delOptions = {bundle.getString("confirm"), bundle.getString("cancel")};
          String delWarningLabel = bundle.getString("deleteConnectionLabel");
          String delWarning = bundle.getString("deleteConnectionWarning");
          int selectedDelOption = JOptionPane.showOptionDialog(null, delWarning, delWarningLabel, 
                               JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                               null, delOptions, delOptions[0]);
          if (selectedDelOption == 0) {
            if (disconnectButton.isEnabled()) {
              Object[] options = {bundle.getString("ok"), bundle.getString("cancel")};
              String warningLabel = bundle.getString("closeConnectionLabel");
              String warning = bundle.getString("closeConnectionWarning");
              int selectedOption = JOptionPane.showOptionDialog(null, warning, warningLabel, 
                                   JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                   null, options, options[0]);
              if (selectedOption == 0) {
                thisPanel.closeConnection();
              }
              else {
                cancel = true;
              }
            }
          }
          else {
            cancel = true;
          }

          if (!cancel) {
            connectionsCombo.removeItemAt(selIndex);
            thisPanel.storeConnections();
          }
        }
      }
    });

    queryArea.addCaretListener(new CaretListener() {
      public void caretUpdate(CaretEvent ce)
      {
        queryButton.setEnabled(queryArea.getSelectedText() != null &&
                               disconnectButton.isEnabled());
      }
    });
    
    queryArea.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent me)
      {
        if (queryButton.isEnabled())
        {
          JPopupMenu popup = new JPopupMenu();
          JMenuItem queryMenuItem = popup.add(new JMenuItem(bundle.getString("executeButton")));
          queryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
              thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
              // store all queries now:
              thisPanel.storeQueries();
              String outputString = "";
              String queryString = queryArea.getSelectedText();
              try {
                if (queryString.toLowerCase().startsWith("select")) {
                  queryExecutor = new QueryExecutor("name", stmt, queryString, thisPanel);
                  queryExecutor.start();
                  cancelQueryButton.setEnabled(true);
                }
                else {
                  int numRows = stmt.executeUpdate(queryString);
                  outputString += "(" + numRows + " row(s) ";
                  if (queryString.toLowerCase().startsWith("insert")) {
                    outputString += "inserted)\n";
                  }
                  else if (queryString.toLowerCase().startsWith("update")) {
                    outputString += "updated)\n";
                  }
                  else if (queryString.toLowerCase().startsWith("delete")) {
                    outputString += "deleted)\n";
                  }
                  outputArea.setText(outputString);
                  thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
              } catch (SQLException sqle) {
                outputString = "SQLException executing query: '" + sqle.getMessage() + "'";
              }
              queryArea.setSelectionEnd(queryArea.getSelectionStart());
              queryButton.setEnabled(false);
            }
          });
          popup.show(queryArea, me.getX(), me.getY());
        }
      }
    });
    
    metadataTree.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent me)
      {
        int mouseRow = metadataTree.getRowForLocation(me.getX(), me.getY());
        final TreePath mousePath = metadataTree.getPathForLocation(me.getX(), me.getY());
        if (mouseRow != -1) {
          metadataTree.setSelectionPath(mousePath);
          if (mousePath.getPathCount() == 2 &&
              mousePath.getPathComponent(0).toString().equals("Metadata") &&
              ((me.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)) {
            // pop context menu to provide row count and table scan choices:
            JPopupMenu popup = new JPopupMenu();
            JMenuItem rowCount = popup.add(new JMenuItem("Row Count"));
            rowCount.addActionListener(new ActionListener()
            {
              public void actionPerformed(ActionEvent ae)
              {
                thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                String queryString = "SELECT COUNT(*) FROM " + 
                                     mousePath.getPathComponent(1).toString().toUpperCase();
                queryExecutor = new QueryExecutor("name", stmt, queryString, thisPanel);
                queryExecutor.start();
                cancelQueryButton.setEnabled(true);
              }
            });
            JMenuItem tableScan = popup.add(new JMenuItem("Return All Rows"));
            tableScan.addActionListener(new ActionListener()
            {
              public void actionPerformed(ActionEvent ae)
              {
                thisPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                String queryString = "SELECT * FROM " + 
                                     mousePath.getPathComponent(1).toString().toUpperCase();
                queryExecutor = new QueryExecutor("name", stmt, queryString, thisPanel);
                queryExecutor.start();
                cancelQueryButton.setEnabled(true);
              }
            });
            popup.show(metadataTree, me.getX(), me.getY());
          }
        }
      }
    });
  }
  
  protected void loadQueries() {
    
    // load file of queries saved from last exit:
    try {
//        BufferedReader br = new BufferedReader(new FileReader("com/adamsresearch/jdbcquerytool/queries.config"));
      BufferedReader br = new BufferedReader(new InputStreamReader(
              this.getClass().getResourceAsStream("queries.config")));
      String nextLine = "";
      while ((nextLine = br.readLine()) != null) {
        queryArea.append(nextLine + "\n");
      }
    }
    catch (FileNotFoundException fnfe) {
      System.out.println("Did not find a \"queries.config\" file; will be creating a new one.");
    }
    catch (IOException ioe) {
      System.out.println("IOException reading saved queries: '" + ioe.getMessage() + "'");
    }
  }
  
  protected void storeQueries() {
    
    // store in "queries.config" file
    try {
      PrintWriter pw = new PrintWriter(new FileWriter("queries.config"), true);
      pw.println(queryArea.getText());
    }
    catch (IOException ioe) {
      System.out.println("IOException saving your queries: '" + ioe.getMessage() + "'");
    }
  }
  
  protected void storeConnections() {
    
    // store in properties file
    Properties newConnProps = new Properties();
    for (int i=0; i<connectionsCombo.getItemCount(); i++) {
      DatabaseConnection nextConn = (DatabaseConnection)(connectionsCombo.getItemAt(i));
      newConnProps.setProperty(nextConn.getConnectionName(),  nextConn.connectionInfo());
    }
    try {
      newConnProps.store(new FileOutputStream("com/adamsresearch/jdbcquerytool/connections.config"), "connection properties");
    }
    catch (IOException ioe) {
      System.out.println("IOException writing out connection configuration: '" + ioe.getMessage() + "'");
    }
  }
  
  public void closeConnection() {
    if ((disconnectButton.isEnabled()) && (stmt != null) && (conn != null))
    {
      try {
        stmt.close();
      } catch (SQLException sqle1a) {
        System.out.println("SQLException closing Statement: '" + sqle1a.getMessage() + "'");
        SQLException sqle1b = sqle1a.getNextException();
        if (sqle1b != null)
        {
          System.out.println("Embedded exception: '" + sqle1b.getMessage() + "'");
        }
      }
      try {
        conn.close();
      } catch (SQLException sqle2a) {
        System.out.println("SQLException closing Connection: '" + sqle2a.getMessage() + "'");
        SQLException sqle2b = sqle2a.getNextException();
        if (sqle2b != null)
        {
          System.out.println("Embedded exception: '" + sqle2b.getMessage() + "'");
        }
      }
      thisPanel.clearDatabaseMetadata();
      queryButton.setEnabled(false);
      connectButton.setEnabled(true);
      disconnectButton.setEnabled(false);
    }
  }
  
  public void returnResultSet(ResultSet resultSet)
  {
    String outputString = "";
    try
    {
      ResultSetMetaData rsmd = resultSet.getMetaData();
      outputString += thisPanel.getQueryMetaData(rsmd);
      int numberOfColumns = rsmd.getColumnCount();
      int typesArray[] = new int[numberOfColumns];
      // get column types:
      for (int col=0; col<numberOfColumns; col++) {
        typesArray[col] = rsmd.getColumnType(col+1);
      }
      while(resultSet.next()) {
        for (int i=1; i<=numberOfColumns; i++) {
          outputString += thisPanel.getColumnValue(resultSet, i, typesArray[i-1]);
          if (i != numberOfColumns) {
            outputString += ", ";
          }
        }
        outputString += "\n";
      }
      outputArea.setText(outputString);
      cancelQueryButton.setEnabled(false);
      thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    catch (SQLException sqle)
    {
      System.err.println(sqle.getMessage());
    }
  }
  
  protected void setErrorMessage(Exception exc)
  {
      outputArea.setText(exc.getMessage());
      cancelQueryButton.setEnabled(false);
      thisPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  }
  
  protected void setDatabaseMetadata() 
  {
    ResultSet tablesRS = null;
    ResultSet columnsRS = null;
    ResultSet pKeysRS = null;
    ResultSet fKeysRS = null;
    try 
    {
      DatabaseConnection dConn = (DatabaseConnection)(connectionsCombo.getSelectedItem());
      String schemaName = dConn.getUsername();
      dbMetaData = conn.getMetaData();
      // create a new tree model and update the metdata panel:
      schemaNameStrategy = CASE_SENSITIVE_SCHEMA_NAME_STRATEGY;
      tablesRS = dbMetaData.getTables(null, schemaName, null, null);
      final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Metadata");
      while (tablesRS.next())
      {
        String tableName = tablesRS.getString(3);
        rootNode.add(new DefaultMutableTreeNode(tableName));
      }
      tablesRS.close();
      // if that schema name failed, try again promoting the schema name to upper case:
      if (rootNode.getChildCount() == 0)
      {
        schemaNameStrategy = CASE_INSENSITIVE_SCHEMA_NAME_STRATEGY;
        tablesRS = dbMetaData.getTables(null, dConn.getUsername().toUpperCase(), null, null);
        while (tablesRS.next())
        {
          String tableName = tablesRS.getString(3);
          rootNode.add(new DefaultMutableTreeNode(tableName));
        }
        tablesRS.close();
        // if that schema failed, maybe we're using a DBMS where the schema should be null:
        if (rootNode.getChildCount() == 0)
        {
          schemaNameStrategy = NULL_SCHEMA_NAME_STRATEGY;
          // note this is typically SQL Server, maybe Sybase too;
          // we also have to explicitly filter out "SYSTEM TABLE"
          // table types:
          String[] tableTypes = new String[] {"TABLE", "VIEW", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};
          tablesRS = dbMetaData.getTables(null, null, null, tableTypes);
          while (tablesRS.next())
          {
            String tableName = tablesRS.getString(3);
            rootNode.add(new DefaultMutableTreeNode(tableName));
          }
          tablesRS.close();
        }
      }
      // now that we know how many tables there are, switch the JProgressBar from
      // indeterminate to determinate mode and update its value for each element
      // in the table enumeration:
      numberOfTables = rootNode.getChildCount() - 1;
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          progressBar.setMinimum(0);
          progressBar.setMaximum(numberOfTables);
          progressBar.setIndeterminate(false);
        }
      });
      
      // to show some intermediate progress filling the metadata
      // tree, populate it with the table names now:
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          metadataTreeModel = new DefaultTreeModel(rootNode);
          metadataTree.setModel(metadataTreeModel);
        }
      });

      // iterate over the table nodes and add column nodes with types
      Enumeration tableNodes = rootNode.children();
      currentTableNumber = 0;
      while (tableNodes.hasMoreElements())
      {
          // update the progress bar:
          SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
              progressBar.setValue(currentTableNumber);
            }
          });
          currentTableNumber++;
          DefaultMutableTreeNode tableNode = (DefaultMutableTreeNode)tableNodes.nextElement();
          String tableName = tableNode.toString();
          switch (schemaNameStrategy)
          {
            case CASE_SENSITIVE_SCHEMA_NAME_STRATEGY:
              pKeysRS = dbMetaData.getPrimaryKeys(null, schemaName, tableName);
              break;
              
            case CASE_INSENSITIVE_SCHEMA_NAME_STRATEGY:
              pKeysRS = dbMetaData.getPrimaryKeys(null, schemaName.toUpperCase(), tableName);
              break;
              
            case NULL_SCHEMA_NAME_STRATEGY:
              pKeysRS = dbMetaData.getPrimaryKeys(null, null, tableName);
          }
          HashMap<String, String> pKeys = new HashMap<String, String>();
          while (pKeysRS.next())
          {
              pKeys.put(pKeysRS.getString(4), pKeysRS.getString(6));
          }
	        pKeysRS.close();
          switch (schemaNameStrategy)
          {
            case CASE_SENSITIVE_SCHEMA_NAME_STRATEGY:
              fKeysRS = dbMetaData.getImportedKeys(null, schemaName, tableName);
              break;
              
            case CASE_INSENSITIVE_SCHEMA_NAME_STRATEGY:
              fKeysRS = dbMetaData.getImportedKeys(null, schemaName.toUpperCase(), tableName);
              break;
              
            case NULL_SCHEMA_NAME_STRATEGY:
              fKeysRS = dbMetaData.getImportedKeys(null, null, tableName);
          }
          Vector<ForeignKey> fKeys = new Vector<ForeignKey>(0);
          while (fKeysRS.next())
          {
              String forTblName = fKeysRS.getString(3);
              String forTblColName = fKeysRS.getString(4);
              String colName = fKeysRS.getString(8);
              String fkName = fKeysRS.getString(12);
              ForeignKey fk = new ForeignKey(colName, fkName, forTblName, forTblColName);
              fKeys.addElement(fk);
          }
	        fKeysRS.close();
          switch (schemaNameStrategy)
          {
            case CASE_SENSITIVE_SCHEMA_NAME_STRATEGY:
              columnsRS = dbMetaData.getColumns(null, schemaName, tableName, null);
              break;
              
            case CASE_INSENSITIVE_SCHEMA_NAME_STRATEGY:
              columnsRS = dbMetaData.getColumns(null, schemaName.toUpperCase(), tableName, null);
              break;
              
            case NULL_SCHEMA_NAME_STRATEGY:
              columnsRS = dbMetaData.getColumns(null, null, tableName, null);
          }
          while (columnsRS.next())
          {
              String columnName = columnsRS.getString(4);
              String columnType = columnsRS.getString(6);
              String columnData = columnName+ " (" + columnType + ")";
              DefaultMutableTreeNode columnNode = new DefaultMutableTreeNode(columnData);
              if (pKeys.containsKey(columnName))
              {
                  String pkName = (String)pKeys.get(columnName);
                  String pkData = "PK";
                  if (pkName != null)
                  {
                      pkData += " (" + pKeys.get(columnName) + ")";
                  }
                  DefaultMutableTreeNode pkNode = new DefaultMutableTreeNode(pkData);
                  columnNode.add(pkNode);
              }
              // see if this column's a foreign key
              for (int i=0; i<fKeys.size(); i++)
              {
                  ForeignKey fk = fKeys.elementAt(i);
                  if (fk.getColumnName().equals(columnName))
                  {
                      String fkData = "FK (" + fk.getForeignKeyName() + ") on " + fk.getForeignTableName() + "." + fk.getForeignTableColumnName();
                      DefaultMutableTreeNode fkNode = new DefaultMutableTreeNode(fkData);
                      columnNode.add(fkNode);
                  }
              }
              tableNode.add(columnNode);
          }
          columnsRS.close();
      }
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        metadataTreeModel = new DefaultTreeModel(rootNode);
        metadataTree.setModel(metadataTreeModel);
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
      }
    });
    }
    catch (SQLException sqle) 
    {
      final String msg = sqle.getMessage();
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          outputArea.setText("SQLException getting metadata; message is '" + msg + "'");
        }
      });
    }
    catch (Exception exc) 
    {
      final String exceptionName = exc.getClass().getName();
      final String msg = exc.getMessage();
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          outputArea.setText(exceptionName + " getting metadata; message is '" + msg + "'");
        }
      });
    }
    finally
    {
        try
        {
	    try
	    {
              pKeysRS.close();
	    }
	    catch (Exception pkRSExc) {}
	    try
	    {
              fKeysRS.close();
	    }
	    catch (Exception fkRSExc) {}
	    try
	    {
              columnsRS.close();
	    }
	    catch (Exception cRSExc) {}
	    try
	    {
              tablesRS.close();
	    }
	    catch (Exception tRSExc) {}
        }
        catch (Exception exc)
        {
        }
        outputArea.setText("");
    }
  }
  
  protected void clearDatabaseMetadata()
  {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        metadataTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Metadata"));
        metadataTree.setModel(metadataTreeModel);
        metadataSplitPane.setDividerLocation(0.0);
      }
    });
  }
  
  protected String getQueryMetaData(ResultSetMetaData rsmd) {
    String mdString = "";
    try {
      int numCols = rsmd.getColumnCount();
      for (int i=1; i<=numCols; i++) {
//      mdString += rsmd.getColumnName(i).toLowerCase();
        mdString += rsmd.getColumnName(i);
        if (i < numCols) {
          mdString += ", ";
        }
      }
      mdString += "\n\n";
      return mdString;
    }
    catch (SQLException sqle) {
      outputArea.setText("SQLException getting columns; message is '" + sqle.getMessage() + "'");
      return "(error retrieving column names)\n";
    }
    
  }
  
  protected String getColumnValue(ResultSet rs, int column, int sqlType) {
    try {
      // try to catch all the numeric ones so they're not quoted:
      if ((sqlType == Types.TINYINT) || (sqlType == Types.INTEGER) ||
          (sqlType == Types.BIGINT) || (sqlType == Types.BIT) ||
          (sqlType == Types.DECIMAL) || (sqlType == Types.DOUBLE) ||
          (sqlType == Types.FLOAT) || (sqlType == Types.INTEGER) ||
          (sqlType == Types.REAL) || (sqlType == Types.SMALLINT) ||
          (sqlType == Types.NUMERIC)) {
          return rs.getString(column);
      }
      else if (sqlType == Types.CLOB) {
System.out.println("  got a clob!");
        return "'" + rs.getClob(column) + "'";
      }
      else {
        return "'" + rs.getString(column) + "'";
      }
    }
    catch (SQLException sqle) {
      outputArea.setText("SQLException getting column value: '" + sqle.getMessage() + "'");
      return "<error>";
    }
  }
  
  public class ForeignKey
  {
      protected String columnName = "";
      protected String foreignKeyName = "";
      protected String foreignTableName = "";
      protected String foreignTableColumnName = "";
      
      public ForeignKey(String col, String fkName, String forTbl, String forTblCol)
      {
          columnName = col;
          foreignKeyName = fkName;
          foreignTableName = forTbl;
          foreignTableColumnName = forTblCol;
      }
      
      public String getColumnName()
      {
          return columnName;
      }
      public String getForeignKeyName()
      {
          return foreignKeyName;
      }
      public String getForeignTableName()
      {
          return foreignTableName;
      }
      public String getForeignTableColumnName()
      {
          return foreignTableColumnName;
      }
  }
  
  public class ConnectionDialog extends JDialog {
    
    protected ConnectionDialog thisDialog;
    protected Container contentPane;
    protected boolean selectedOK = true;
    protected ResourceBundle bundle;
    protected DatabaseConnection dbConns[];
    protected DatabaseConnection dbConn;

    protected JLabel nameLabel;
    protected JTextField nameField;
    protected JLabel driverLabel;
    protected JTextField driverField;
    protected JLabel URLLabel;
    protected JTextArea URLArea;
    protected JLabel usernameLabel;
    protected JTextField usernameField;
    protected JLabel passwordLabel;
    protected JPasswordField passwordField;
    protected JLabel propertiesStringLabel;
    protected JTextArea propertiesStringArea;
    protected JButton okButton;
    protected JButton cancelButton;
    
    public ConnectionDialog(ResourceBundle rb, DatabaseConnection dbc) {
      thisDialog = this;
      contentPane = thisDialog.getContentPane();
      bundle = rb;
      dbConn = dbc;
      
      thisDialog.setTitle(bundle.getString("connectDialog.dialogLabel"));
      thisDialog.setModal(true);
      layOutDialog();
      initializeDialog();
      addListeners();
    }
    
    protected void layOutDialog() {
      
      nameLabel = new JLabel(bundle.getString("connectDialog.nameLabel"));
      nameField = new JTextField();
      nameField.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      driverLabel = new JLabel(bundle.getString("connectDialog.driverLabel"));
      driverField = new JTextField();
      driverField.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      URLLabel = new JLabel(bundle.getString("connectDialog.URLLabel"));
      URLArea = new JTextArea();
      URLArea.setLineWrap(true);
      URLArea.setWrapStyleWord(true);
      URLArea.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      usernameLabel = new JLabel(bundle.getString("connectDialog.usernameLabel"));
      usernameField= new JTextField();
      usernameField.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      passwordLabel = new JLabel(bundle.getString("connectDialog.passwordLabel"));
      passwordField = new JPasswordField();
      passwordField.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      propertiesStringLabel = new JLabel(bundle.getString("connectDialog.propertiesStringLabel"));
      propertiesStringArea = new JTextArea();
      propertiesStringArea.setLineWrap(true);
      propertiesStringArea.setWrapStyleWord(true);
      propertiesStringArea.setFont(new Font("Monospaced", Font.BOLD | Font.PLAIN, 12));
      okButton = new JButton(bundle.getString("connectDialog.okButton"));
      cancelButton = new JButton(bundle.getString("connectDialog.cancelButton"));
      
      GridBagLayout gbl = new GridBagLayout();
      contentPane.setLayout(gbl);
      GridBagConstraints gbc = new GridBagConstraints();
      
      gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(nameLabel, gbc);
      
      gbc.gridx = 0; gbc.gridy = 1; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
      contentPane.add(nameField, gbc);
      
      gbc.gridx = 0; gbc.gridy = 2; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(driverLabel, gbc);
      
      gbc.gridx = 0; gbc.gridy = 3; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
      contentPane.add(driverField, gbc);
      
      gbc.gridx = 0; gbc.gridy = 4; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(URLLabel, gbc);
      
      gbc.gridx = 0; gbc.gridy = 5; gbc.gridheight = 1; gbc.gridwidth = 3;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 0.5; gbc.fill = GridBagConstraints.BOTH;
      contentPane.add(URLArea, gbc);
      
      gbc.gridx = 0; gbc.gridy = 6; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(usernameLabel, gbc);
      
      gbc.gridx = 0; gbc.gridy = 7; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
      contentPane.add(usernameField, gbc);
      
      gbc.gridx = 1; gbc.gridy = 6; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(passwordLabel, gbc);
      
      gbc.gridx = 1; gbc.gridy = 7; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
      contentPane.add(passwordField, gbc);
      
      gbc.gridx = 0; gbc.gridy = 8; gbc.gridheight = 1; gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(propertiesStringLabel, gbc);
      
      gbc.gridx = 0; gbc.gridy = 9; gbc.gridheight = 3; gbc.gridwidth = 3;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
      contentPane.add(propertiesStringArea, gbc);
      
      // dismiss buttons:
      JPanel dismissButtonsPanel = new JPanel();
      dismissButtonsPanel.setLayout(new GridLayout(1, 2));
      dismissButtonsPanel.add(okButton);
      dismissButtonsPanel.add(cancelButton);
      gbc.gridx = 1; gbc.gridy = 12; gbc.gridheight = 1; gbc.gridwidth = 2;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE;
      contentPane.add(dismissButtonsPanel, gbc);

      thisDialog.setSize(400, 350);
    }
    
    protected void initializeDialog() {
      nameField.setText(dbConn.getConnectionName());
      driverField.setText(dbConn.getDriverClassName());
      URLArea.setText(dbConn.getDatabaseUrl());
      usernameField.setText(dbConn.getUsername());
      passwordField.setText(dbConn.getPassword());
      propertiesStringArea.setText(dbConn.getPropertiesString());
    }
    
    protected void addListeners() {
      okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          dbConn.setConnectionName(nameField.getText());
          dbConn.setDriverClassName(driverField.getText());
          dbConn.setDatabaseUrl(URLArea.getText());
          dbConn.setUsername(usernameField.getText());
          char[] passArray = passwordField.getPassword();
          dbConn.setPassword(new String(passArray));
          for (int i=0; i<passArray.length; i++) {
            passArray[i] = ' ';
          }
          dbConn.setPropertiesString(propertiesStringArea.getText());
          selectedOK = true;
          thisDialog.setVisible(false);
        }
      });
          
      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          selectedOK = false;
          thisDialog.setVisible(false);
        }
      });
    }
    
    public DatabaseConnection getDatabaseConnection() {
      return dbConn;
    }
    
    public boolean selectedOK() {
      return selectedOK;
    }
  }
  
  public class DatabaseConnection {
    
    protected String connectionName = "";
    protected String driverClassName = "";
    protected String databaseUrl = "";
    protected String username = "";
    protected String password = "";
    protected String propertiesString = "";
    
    public DatabaseConnection() {
    }
    
    public DatabaseConnection(String connName, String driverName,
                              String url, String propsString) {
      connectionName = connName;
      driverClassName = driverName;
      databaseUrl = url;
      propertiesString = propsString;
    }
    
    public DatabaseConnection(String connName, String driverName,
                              String url, String user, String pass) {
      connectionName = connName;
      driverClassName = driverName;
      databaseUrl = url;
      username = user;
      password = pass;
    }
    
    public DatabaseConnection(String connName, String stringForm) 
    {
      driverClassName = "";
      databaseUrl = "";
      username = "";
      password = "";
      propertiesString = "";
      try
      {
        connectionName = connName;
        // need "true" to handle null inputs
        StringTokenizer connST = new StringTokenizer(stringForm, "|", true);
        if (connST.hasMoreTokens()) {
          driverClassName = connST.nextToken();
        }
        connST.nextToken();
        if (connST.hasMoreTokens()) {
          databaseUrl = connST.nextToken();
        }
        connST.nextToken();
        if (connST.hasMoreTokens()) {
          String token = connST.nextToken();
          if (!token.equals("|")) {
            username = token;
            connST.nextToken();
          }
          else {
            username = "";
          }
        }
        if (connST.hasMoreTokens()) {
          String token = connST.nextToken();
          if (!token.equals("|")) {
            password = token;
            connST.nextToken();
          }
          else {
            password = "";
          }
        }
        if (connST.hasMoreTokens()) {
          String token = connST.nextToken();
          if (!token.equals("|")) {
            propertiesString = token;
          }
        }
      }
      catch (NoSuchElementException nsee)
      {
        System.err.println("\nError parsing connections file:");
        System.err.println("  Connection name = '" + connName + "'");
        System.err.println("  String representation = '" + stringForm + "'");
        System.err.println("  driverClassName = '" + driverClassName + "'");
        System.err.println("  databaseUrl = '" + databaseUrl + "'");
        System.err.println("  username = '" + username + "'");
        System.err.println("  password = '" + password + "'");
        System.err.println("  propertiesString = '" + propertiesString + "'");
        System.exit(-1);
      }
    }
    
    public void setConnectionName(String newName) {
      connectionName = newName;
    }
    public String getConnectionName() {
      return connectionName;
    }
    public void setDriverClassName(String newName) {
      driverClassName = newName;
    }
    public String getDriverClassName() {
      return driverClassName;
    }
    public void setDatabaseUrl(String newUrl) {
      databaseUrl = newUrl;
    }
    public String getDatabaseUrl() {
      return databaseUrl;
    }
    public void setUsername(String newUsername) {
      username = newUsername;
    }
    public String getUsername() {
      return username;
    }
    public void setPassword(String newPassword) {
      password = newPassword;
    }
    public String getPassword() {
      return password;
    }
    public void setPropertiesString(String newProps) {
      propertiesString = newProps;
    }
    public String getPropertiesString() {
      return propertiesString;
    }
    public Properties getDbProperties() {
      Properties props = new Properties();
      StringTokenizer st = new StringTokenizer(propertiesString, ";");
      while (st.hasMoreTokens()) {
        try {
          String keyValuePair = (String)st.nextToken();
          StringTokenizer kvst = new StringTokenizer(keyValuePair, "=");  // only has 2
          try {
            String property = kvst.nextToken();
            String value = kvst.nextToken();
            props.setProperty(property, value);
          } catch (NoSuchElementException nsee_1) {
            System.out.println("NoSuchElementException parsing string '" + keyValuePair + "'");
          }
        } catch (NoSuchElementException nsee_2) {
          System.out.println("NoSuchElementException parsing string '" + propertiesString + "'");
        }
      }
      return props;
    }
    
    /**
     * Return Stringified form of the DatabaseConnection
     * which is suitable for putting into a java.util.Properties
     * file.  Names must be unique to avoid overwrite.  Then the
     * name of the connection is the key and the rest of it can
     * be passed to the 2-String constructor.  Uses pipe ("|")
     * symbol as delimiter; if your database name, driver class
     * name, url or properties strings use this character, you'll
     * not be able to instantiate a DatabaseConnection from the
     * output of this method.  Because a Properties entry is created
     * by setting a property with a String, this method only returns
     * the non-connectionName portion of the DatabaseConnection; you
     * will set property "connectionName" to value "connectionInfo()".
     */
    public String connectionInfo() {
      return driverClassName + "|" + databaseUrl + "|" + username +
             "|" + password + "|" + propertiesString;
    }
    
    public String toString() {
      return connectionName;
    }
  }
  
  public class QueryExecutor extends Thread
  {
    protected Statement statement = null;
    protected String queryString = null;
    protected Query parent;
    
    public QueryExecutor(String name, Statement stmt, String query, Query queryApp)
    {
      super(name);
      statement = stmt;
      queryString = query;
      parent = queryApp;
    }

    public void run()
    {
      try
      {
        ResultSet rs = stmt.executeQuery(queryString);
        parent.returnResultSet(rs);
      }
      catch (SQLException sqle)
      {
        parent.setErrorMessage(sqle);
      }
      catch (Exception exc)
      {
        parent.setErrorMessage(exc);
      }
    }
    
    public void cancelQuery()
    {
      try
      {
        statement.cancel();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            outputArea.setText("Cancelled query");
            cancelQueryButton.setEnabled(false);
            parent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
          }
        });
      }
      catch (SQLException sqle)
      {
        System.err.println(sqle.getMessage());
      }
    }
  }
}

