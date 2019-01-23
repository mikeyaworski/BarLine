<?php

require_once __DIR__ . '/api_abstract.php';
require_once __DIR__ . '/db_connect.php';
require_once __DIR__ . '/helpers.php';

class BarlineAPI extends API {
  private $password;

  public function __construct($request, $password) {
    parent::__construct($request);

    $this->password = $password;
  }

  // endpoint function
  protected function password_getter($args, $payload) {
    if ($this->method == 'GET') {
      if (sizeof($args) > 0) {
        $passStr = $args[0];
        return array('salted hash' => password_hash($passStr, PASSWORD_DEFAULT));
      }
    }
  }


  // endpoint function
  protected function status($args) {
    $db = new DB();
    $response = array();

    // prepare and bind
    $stmt = $db->stmt_init();
    $stmt->prepare("SELECT * FROM `API_Statuses` WHERE `version`='temp'");

    if ($stmt->execute()) {
      $result = $stmt->get_result();
      $response = $result->fetch_array(MYSQLI_ASSOC);
    } else {
      $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
    }

    $stmt->close();
    $db->close();

    return $response;
  }

  // endpoint function
  protected function cities($args) {
    $db = new DB();
    $response = array();
    $province = NULL;

    if (isset($_GET['province'])) $province = $_GET['province'];

    // prepare and bind
    $stmt = $db->stmt_init();
    if ($province != NULL) {
      $stmt->prepare("SELECT `city` FROM `Bars` WHERE `province`=? ORDER BY city ASC");
      $stmt->bind_param("s", $province);
    } else {
      $stmt->prepare("SELECT `city` FROM `Bars` ORDER BY city ASC");
    }

    if ($stmt->execute()) {
      $result = $stmt->get_result();
      while ($e = $result->fetch_array(MYSQLI_ASSOC)) {
        $cityObj = array('name' => $e['city']);
        if (!in_array($cityObj, $response)) {
          $response[] = $cityObj;
        }
      }
    } else {
      $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
    }

    $stmt->close();
    $db->close();

    return $response;
  }

  // endpoint function
  protected function bars($args) {
    $db = new DB();

    $response = array();

    $city = NULL;
    $province = NULL;

    if (isset($_GET['city'])) $city = $_GET['city'];
    if (isset($_GET['province'])) $province = $_GET['province'];

    $querySelect = "SELECT `id`,`name` FROM `Bars`";
    $queryOrder = " ORDER BY name ASC";

    // prepare and bind
    $stmt = $db->stmt_init();
    if ($province != NULL && $city != NULL) {
      $stmt->prepare($querySelect . " WHERE `city`=? AND `province`=?" . $queryOrder);
      $stmt->bind_param("ss", $city, $province);
    } else if ($city != NULL) {
      $stmt->prepare($querySelect . " WHERE `city`=?" . $queryOrder);
      $stmt->bind_param("s", $city);
    } else if ($province != NULL) {
      $stmt->prepare($querySelect . " WHERE `province`=?" . $queryOrder);
      $stmt->bind_param("s", $province);
    } else {
      $stmt->prepare($querySelect . $queryOrder);
    }

    if ($stmt->execute()){
      $result = $stmt->get_result();
      while ($e = $result->fetch_array(MYSQLI_ASSOC)) {
        if (!in_array($e, $response)) {
          $response[] = $e;
        }
      }
      //$response = array('bars' => $response);
    } else {
      $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
    }

    $stmt->close();
    $db->close();

    return $response;
  }

  // endpoint function
  protected function auth($args) {
    if (isset($this->password)) {
      //$passwordHash = md5($this->password);
      if (sizeof($args) > 0) {
        $barId = $args[0];
        $db = new DB();
        $response = array();

        // prepare and bind
        $stmt = $db->stmt_init();
        $stmt->prepare("SELECT `password` FROM `Bars` WHERE `id`=?");
        $stmt->bind_param("i", $barId);

        if ($stmt->execute()){
          $result = $stmt->get_result();
          while ($e = $result->fetch_array(MYSQLI_ASSOC)) {
            if (password_verify($this->password, $e["password"])) {
            //if ($e["password"] == $passwordHash) {
              $stmt->close();
              $db->close();
              return array("auth_status" => 1, "message" => "Correct password.");
            }
          }
          $response = array("auth_status" => 0, "message" => "Incorrect password.", 'http_request_status' => 401);
        } else {
          $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
        }

        $stmt->close();
        $db->close();

        return $response;

      } else {
        return array('error' => 'Need bar id', 'http_request_status' => 400);
      }
    } else {
      return array('error' => 'Need password', 'http_request_status' => 401);
    }
  }

  // endpoint function
  protected function wait_time($args, $payload) {
    if ($this->method == 'GET') {
      if (sizeof($args) > 0) {
        $barId = $args[0];
        $db = new DB();
        $response = array();

        // prepare and bind
        $stmt = $db->stmt_init();
        $stmt->prepare("SELECT `id`,`name`,`wait_time`,`crowding_level`,`last_updated`,`ad_img`,`ad_text`,`ad_link` FROM `Bars` WHERE `id`=? ORDER BY name ASC");
        $stmt->bind_param("i", $barId);

        if ($stmt->execute()) {
          $result = $stmt->get_result();
          $response = $result->fetch_array(MYSQLI_ASSOC);

          $this->logWaitCall($response['id'], $response['wait_time'], $response['crowding_level'], $response['ad_img'], $response['ad_text'], $response['ad_link']);
        } else {
          $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
        }

        $stmt->close();
        $db->close();

        return $response;
      } else {
        return array('error' => 'Need bar id', 'http_request_status' => 400);
      }
    } else if ($this->method == 'PUT') {
      if (sizeof($args) > 0) {
        $barId = $args[0];
        if ($payload != NULL) {
          if ($this->auth($args)['auth_status'] == 1) {
            $db = new DB();
            $response = array();

            // prepare and bind
            $stmt = $db->stmt_init();

            $waitTime = NULL;
            $crowdingLevel = NULL;

            $queryUpdate = "UPDATE `Bars` SET";
            $queryWhere = " WHERE `id`=?";

            $json = json_decode($payload, true);

            if (isset($json['waitTime']) && isset($json['crowdingLevel'])) {
              $waitTime = $json['waitTime'];
              $crowdingLevel = $json['crowdingLevel'];

              $stmt->prepare($queryUpdate . " `wait_time`=?, `crowding_level`=?, `last_updated`=NOW()" . $queryWhere);
              $stmt->bind_param("iii", $waitTime, $crowdingLevel, $barId);
            } else if (isset($json['waitTime'])) {
              $waitTime = $json['waitTime'];

              $stmt->prepare($queryUpdate . " `wait_time`=?, `last_updated`=NOW()" . $queryWhere);
              $stmt->bind_param("ii", $waitTime, $barId);
            } else if (isset($json['crowdingLevel'])) {
              $crowdingLevel = $json['crowdingLevel'];

              $stmt->prepare($queryUpdate . " `crowding_level`=?, `last_updated`=NOW()" . $queryWhere);
              $stmt->bind_param("ii", $crowdingLevel, $barId);
            } else {
              $response = array('error' => 'Wait time and/or crowding level required in the request body', 'http_request_status' => 400);

              $stmt->close();
              $db->close();

              return $response;
            }

            $beforeEditResponse = $this->getWaitTimeForLog($barId);
            if ($stmt->execute()) {
              $response = array('success_status' => 1, 'message' => 'Wait Time and Crowding Level successfully updated.');

              $deviceName = '';
              if (isset($json['deviceName'])) {
                $deviceName = $json['deviceName'];
              }

              // log it
              $afterEditResponse = $this->getWaitTimeForLog($barId);
              $this->logEditCall($afterEditResponse['id'], $beforeEditResponse['wait_time'], $afterEditResponse['wait_time'], $beforeEditResponse['crowding_level'], $afterEditResponse['crowding_level'], $beforeEditResponse['last_updated'], $deviceName);

            } else {
              $response = array('error' => 'Error in retrieving database.', 'http_request_status' => 500);
            }

            $stmt->close();
            $db->close();

            return $response;
          } else {
            return array('error' => 'No authorization to complete this action', 'http_request_status' => 401);
          }
        } else {
          return array('error' => 'Wait time and/or crowding level required in the request body', 'http_request_status' => 400);
        }
      } else {
        return array('error' => 'Need bar id', 'http_request_status' => 400);
      }
    } else {
      return array('error' => 'Invalid request method', 'http_request_status' => 404);
    }
  }

  protected function getIp() {
    return $_SERVER['REMOTE_ADDR'];
  }

  protected function getWaitTimeForLog($barId) {
    $db = new DB();
    $response = array();

    // prepare and bind
    $stmt = $db->stmt_init();
    $stmt->prepare("SELECT `id`,`wait_time`,`crowding_level`,`last_updated` FROM `Bars` WHERE `id`=? ORDER BY name ASC");
    $stmt->bind_param("i", $barId);

    if ($stmt->execute()) {
      $result = $stmt->get_result();
      $response = $result->fetch_array(MYSQLI_ASSOC);
    }

    $stmt->close();
    $db->close();

    return $response;
  }

  protected function logWaitCall($id, $waitTime, $crowdingLevel, $adImg, $adText, $adLink) {
    $ip = $this->getIp();
    $db = new DB();

    // prepare and bind
    $stmt = $db->stmt_init();
    $query = "INSERT INTO `Wait_Calls` (`bar_id`, `wait_time`, `crowding_level`, `ad_img`, `ad_text`, `ad_link`, `date`, `ip`) VALUES (?,?,?,?,?,?,NOW(),?)";
    $stmt->prepare($query);
    $stmt->bind_param("iiissss", $id, $waitTime, $crowdingLevel, $adImg, $adText, $adLink, $ip);

    $stmt->execute();

    $stmt->close();
    $db->close();
  }

  protected function logEditCall($id, $previousWaitTime, $newWaitTime, $previousCrowdingLevel, $newCrowdingLevel, $lastUpdated, $deviceName) {
    $ip = $this->getIp();
    $passwordHash = '';
    if (isset($this->password)) {
      $passwordHash = md5($this->password);
    }

    $db = new DB();

    // prepare and bind
    $stmt = $db->stmt_init();
    $query = "INSERT INTO `Edit_Calls` (`bar_id`, `previous_wait_time`, `new_wait_time`, `previous_crowding_level`, `new_crowding_level`, `password`, `date`, `ip`, `last_updated`, `device_name`) VALUES(?,?,?,?,?,?,NOW(),?,?,?)";
    $stmt->prepare($query);
    $stmt->bind_param("iiiiissss", $id, $previousWaitTime, $newWaitTime, $previousCrowdingLevel, $newCrowdingLevel, $passwordHash, $ip, $lastUpdated, $deviceName);

    $stmt->execute();

    $stmt->close();
    $db->close();
  }
}

?>
