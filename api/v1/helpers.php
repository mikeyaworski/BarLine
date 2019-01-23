<?php

function decodeStr($str) {
  return rawurldecode(htmlspecialchars_decode($str));
}

function sqlEscape($str) {
  return mysql_real_escape_string($str);
}

?>
