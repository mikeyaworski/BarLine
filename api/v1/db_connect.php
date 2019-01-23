<?php

// class to be instantiated and connect to the MySQL database
class DB {

  private $con; // connection cursor

  // connect to the database
  public function __construct() {
    require_once __DIR__ . '/db_config.php'; // config file ommitted in repo
    $this->con = new mysqli(DB_SERVER, DB_USERNAME, DB_PASSWORD, DB_DATABASE);
  }

  // delegate stmt_init statement
  public function stmt_init() {
    return $this->con->stmt_init();
  }

  // disconnect from the database
  function close() {
    mysqli_close($this->con);
  }
}

?>
