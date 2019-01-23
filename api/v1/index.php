<?php

require_once __DIR__ . '/api.php';

try {
  $API = new BarlineAPI($_REQUEST['request'], $_SERVER['PHP_AUTH_PW']);
  echo $API->processAPI();
} catch (Exception $e) {
  echo json_encode(array('error' => $e->getMessage()));
}

?>
