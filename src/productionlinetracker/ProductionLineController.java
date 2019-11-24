package productionlinetracker;

import java.io.FileInputStream;

import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

/**
 * Controller Class Handles the the outputs of buttons and connects database. @Author: Nickolas
 * Gadomski
 */
public class ProductionLineController {

  /** Class level fields to be used throughout the code. */
  static Statement stmt = null;

  static Connection conn = null;
  private ObservableList<Product> prods;
  private int prodsIndex = 0;
  private ProductionRec productRec;
  private Employee myEmployee;
  ArrayList<ProductionRec> prodRecordArray;
  //  static PreparedStatement preparedStatement;

  /** Method that starts the connection between the controller and the database. */
  public void initializeDB() {
    final String jdbc_driver = "org.h2.Driver";
    final String db_url = "jdbc:h2:./res/Products";

    final String user = "";
    String pass = "";

    try {
      Properties prop = new Properties();
      prop.load(new FileInputStream("res/properties"));
      pass = prop.getProperty("password");
    } catch (Exception e) {
      System.out.println("Error with database password.");
    }

    System.out.println("Attempting to connect to database.");
    try {
      Class.forName(jdbc_driver);
      conn = DriverManager.getConnection(db_url, user, pass);
      stmt = conn.createStatement();
      System.out.println("Database connection established.");

    } catch (ClassNotFoundException e) {
      e.printStackTrace();

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /** Method to start functionality. */
  @FXML
  public void initialize() throws SQLException {
    cbShowProd.setEditable(true);
    cbShowProd.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    cbShowProd.getSelectionModel().selectFirst();

    for (ItemType item : ItemType.values()) {
      choiceAddProduct.getItems().add(item);
    }

    prods = FXCollections.observableArrayList();

    initializeDB();
    loadProductList();
    setUpProductLineTable();
  }

  /** List of FX identities used for functionality. */
  @FXML private ComboBox<Integer> cbShowProd;

  @FXML private ChoiceBox<ItemType> choiceAddProduct;

  @FXML private TextField tfProductName;

  @FXML private TextField tfManufacturer;

  @FXML private TableView<Product> tvProducts;

  @FXML private TableColumn<?, ?> clProdName;

  @FXML private TableColumn<?, ?> clProdMan;

  @FXML private TableColumn<?, ?> clProdType;

  @FXML private ListView lvChooseProduct;

  @FXML private TextArea taProdLog;

  @FXML private TextArea taEmployeeAccount;

  @FXML private TextField tfFirstName;

  @FXML private PasswordField pfPassword;

  @FXML private Button btnCreateAccount;

  /** add product button on production line tab. */
  @FXML private Button btnAddProduct;

  /** record button on produce tab. */
  @FXML private Button btnRecord;

  /**
   * Method used for the create account button, calls account method.
   *
   * @param event MouseEvent Object
   * @throws SQLException checks if sql statement is valid
   */
  @FXML
  void createAccount(MouseEvent event) throws SQLException {
    account();
  }

  /**
   * Method used for the Add Product button.
   *
   * @param event MouseEvent Object
   */
  @FXML
  void addProduct(MouseEvent event) throws SQLException {
    lvChooseProduct.getItems().clear();
    String prodName = tfProductName.getText();
    String manufacturer = tfManufacturer.getText();
    ItemType type = choiceAddProduct.getValue();

    String productQuery = "INSERT INTO PRODUCT(NAME, TYPE, MANUFACTURER) VALUES (?,?,?)";

    PreparedStatement addProduct = conn.prepareStatement(productQuery);
    addProduct.setString(1, prodName);
    addProduct.setString(2, type.toString());
    addProduct.setString(3, manufacturer);
    addProduct.executeUpdate();

    tfProductName.clear();
    tfManufacturer.clear();
    choiceAddProduct.getSelectionModel().clearSelection();

    prods.add(new Widget(prodName, manufacturer, type));
    lvChooseProduct.getItems().addAll(prods);

    System.out.println("Product Added");
  }

  /**
   * Handles action when button is clicked Outputs to console.
   *
   * @param event MouseEvent object
   */
  @FXML
  void record(MouseEvent event) throws SQLException {
    taProdLog.clear();
    Product selectList = (Product) lvChooseProduct.getSelectionModel().getSelectedItem();
    String quantitys = String.valueOf(cbShowProd.getValue());
    int numberProduced = Integer.parseInt(quantitys); // comes from combo box UI
    ArrayList<ProductionRec> productRecords = new ArrayList();
    for (int runProduct = 0; runProduct < numberProduced; runProduct++) {
      productRec = new ProductionRec(selectList, runProduct);
      productRecords.add(productRec);
    }
    addToProductionDB(productRecords);
    loadProductionLog();
    showProduction();
    System.out.println("Product Recorded");
  }

  /**
   * Method that loads all products from the database and shows it onto the list view.
   *
   * @throws SQLException checks if sql statement is valid
   */
  public void loadProductList() throws SQLException {
    String sql = "SELECT * FROM PRODUCT";
    ResultSet rs = stmt.executeQuery(sql);
    while (rs.next()) {
      // corresponds to database table columns
      String nameDB = rs.getString(2);
      ItemType typeDB = ItemType.valueOf(rs.getString(3));
      String manDB = rs.getString(4);
      // create object
      Product productDB = new Widget(nameDB, manDB, typeDB);
      // save to observable list
      prods.addAll(productDB);
      lvChooseProduct.getItems().add(prods.get(prodsIndex++));
    }
  }

  /**
   * Method that allows the user to add the product from the list view into the production log
   * database. Will not record product unless username is created before hand.
   *
   * @param productionRecArrayList Production Record Array List
   * @throws SQLException checks if sql statement is valid
   */
  public void addToProductionDB(ArrayList<ProductionRec> productionRecArrayList)
      throws SQLException {
    String productQuery =
        "INSERT INTO PRODUCTIONRECORD("
            + "product_name, serial_num, date_produced, USERNAME) VALUES (?,?,?,?)";
    PreparedStatement addRecToDB = conn.prepareStatement(productQuery);
    for (int i = 0; i < productionRecArrayList.size(); i++) {
      addRecToDB.setString(1, productionRecArrayList.get(i).getProductName());
      addRecToDB.setString(2, productionRecArrayList.get(i).getSerialNum());
      addRecToDB.setTimestamp(
          3, new Timestamp(productionRecArrayList.get(i).getProduceDate().getTime()));
      addRecToDB.setString(4, myEmployee.username);
      addRecToDB.executeUpdate();
    }
  }

  /**
   * Method to load the production log from the database.
   *
   * @throws SQLException checks if sql statement is valid
   */
  public void loadProductionLog() throws SQLException {

    prodRecordArray = new ArrayList<>();
    String sql = "SELECT * FROM PRODUCTIONRECORD";
    ResultSet rs = stmt.executeQuery(sql);
    while (rs.next()) {
      // corresponds to database table columns
      int productNumDB = rs.getInt(1);
      String prodNameDB = rs.getString(2);
      String serialNumDB = rs.getString(3);
      Date dateProducedDB = new Date(rs.getTimestamp(4).getTime());
      // create object
      ProductionRec productionDB =
          new ProductionRec(productNumDB, prodNameDB, serialNumDB, dateProducedDB);
      // save to observable list
      prodRecordArray.add(productionDB);
    }
  }

  /** Method to set up the columns for the table view. */
  public void setUpProductLineTable() {
    clProdName.setCellValueFactory(new PropertyValueFactory("prodName"));
    clProdMan.setCellValueFactory(new PropertyValueFactory("manufacturer"));
    clProdType.setCellValueFactory(new PropertyValueFactory("type"));
    tvProducts.setItems(prods);
  }

  /** Method to show the elements of the production log. */
  public void showProduction() {
    for (int i = 0; i < prodRecordArray.size(); i++) {
      taProdLog.appendText(prodRecordArray.get(i).toString() + "\n");
    }
  }

  /**
   * Method to allow the user to create an employee account.
   *
   * @throws SQLException checks if sql statement is valid
   */
  public void account() throws SQLException {
    String name = tfFirstName.getText();
    String password = pfPassword.getText();
    myEmployee = new Employee(name, password);

    String productQuery = "INSERT INTO EMPLOYEE(NAME, PASSWORD, USERNAME, EMAIL) VALUES (?,?,?,?)";
    PreparedStatement addEmployee = conn.prepareStatement(productQuery);
    addEmployee.setString(1, name);
    addEmployee.setString(2, password);
    addEmployee.setString(3, myEmployee.username);
    addEmployee.setString(4, myEmployee.email);
    addEmployee.executeUpdate();

    tfProductName.clear();
    tfManufacturer.clear();
    taEmployeeAccount.appendText(myEmployee.toString() + "\n\n");
  }
}
