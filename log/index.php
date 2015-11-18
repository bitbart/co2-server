<?php
if (isset($_COOKIE["co2logpass"])) {
  if (addslashes($_COOKIE["co2logpass"]) == "puffolandia72")
    header("Location: http://co2.unica.it/log/sweetlogfile.php");
}
?>

<html>
<head>
<title>CO2 Log</title>
</head>

<body style="font-size:12pt; font-family: Arial">
<h1>CO2 Log</h1>
<br>
Please insert your password for accessing this page:

<form method="post" action="sweetlogfile.php">
<input type="password" size="20" name="password" /><br>
<input type="submit" value="Login" name="s1" />
</form>

</body>
</html>