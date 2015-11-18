<?php 

if (isset($_COOKIE["co2logpass"])) {
  if (addslashes($_COOKIE["co2logpass"]) != "puffolandia72")
    die("Access denied. Your attempt has been reported.");
}
else if (isset($_POST["password"])) {
  if (addslashes($_POST["password"]) != "puffolandia72")
    die("Access denied. Your attempt has been reported.");
    
  setcookie("co2logpass", $_POST["password"], time()+(3600*24*365));
}
else
    die("Cannot access this page: go to <a href=\"http://co2.unica.it/log\">co2.unica.it/log</a>.");
?>

<head>
<title>Log of CO2 middleware</title>
<script src="//code.jquery.com/jquery-1.11.2.min.js"></script>
<script src="//code.jquery.com/jquery-migrate-1.2.1.min.js"></script>
<script>
function handleClick(cb) {

  if (cb.name == "warning")
    $(".warningtr").toggle();
  else if (cb.name == "error")
    $(".severetr").toggle();
  else if (cb.name == "minor") {
    $(".finetr").toggle();
    $(".finesttr").toggle();
  }
  else
    $(".infotr").toggle();
}
</script>
<style>
TABLE {
  width: 100%;
  border:1px solid #ccc;
  padding:10px;
  padding-left:5px;
  padding-right:5px;
  border-collapse:separate; 
  border-spacing:4px;
}

TR {
  font-family:Arial; 
  font-size:12pt; 
  height:40px;
  border-bottom: 4px solid #ccc;
}

.spacer {
  display:none;
}

.summary {
  font-size:8pt; 
  color:#666666
}

.new {

  text-align:center;
  width:100px;
  background: #24BAE3;
  margin-right:30px;
  font-size:10pt;
  font-weight:bold;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #aaa;
  text-shadow: 1px 1px 1px #333;
  color: #fff;
}

.info {

  text-align:center;
  width:100px;
  background: #3A9C48;
  margin-right:30px;
  font-size:10pt;
  color:#fff;
  font-weight:bold;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #aaa;
  padding-top:10px;
  text-shadow: 1px 1px 1px #333;
}

.severe {

  text-align:center;
  width:100px;
  background: #E01D1D;
  margin-right:30px;
  font-size:10pt;
  font-weight:bold;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #aaa;
  color:#fff;
  text-shadow: 1px 1px 1px #333;
}

.warning {

  text-align:center;
  width:100px;
  background: #F59E42;
  color: #fff;
  margin-right:30px;
  font-size:10pt;
  font-weight:bold;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #aaa;
  text-shadow: 1px 1px 1px #333;
}

.fine {

  text-align:center;
  width:100px;
  background: #eee;
  margin-right:30px;
  font-size:10pt;
  font-weight:bold;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #ddd;
}

.newspan {

  font-size:11pt;
}

.infospan {

  font-size:11pt;

}

.severespan {
 font-size:11pt;

}

.warningspan {
  font-size:11pt;

}

.newmsg {

  padding:15px; 
  padding-top:6px;
  border-bottom:1px solid #ccc;
  background: #D4F1FA;
  box-shadow: 0px 2px 4px #ddd;
}

.infomsg {

  padding:15px;
  padding-top:6px;
  border-bottom:1px solid #ccc;
  background:#CCEDD1;
  box-shadow: 0px 2px 4px #ddd;
}

.finemsg {

  padding:15px;
  padding-top:6px;
  border-bottom:1px solid #ccc;
  box-shadow: 0px 2px 4px #ddd;
  border-top:1px solid #eee;
}

.warningmsg {

  padding:15px;
  padding-top:6px;
  border-bottom:1px solid #ccc;
  background:#F7D3AD;
  box-shadow: 0px 2px 4px #ddd;
}       

.severemsg {
  padding:15px;
  padding-top:6px;
  border-bottom:1px solid #ccc;
  background:#FAD9D9;
  box-shadow: 0px 2px 4px #ddd;
}

.summary {
  border:1px solid #ccc;
  padding:4px;
  width:500px;
  background:#f3f3f3;
  margin-bottom:10px;
  box-shadow: 2px 2px 4px #999;
}
</style>
</head>
<body style="font-family:Arial; font-size:10pt">
<h1 style="font-family:Georgia; text-shadow:2px 2px 4px #ccc; color:#333">Log of CO2 Middleware</h1>

<div style="padding-bottom:20px; padding-left:10px">
<i>Choose the levels:</i><br><br>
<input type="checkbox" name="error" checked onclick="handleClick(this);"/>Errors<br>
<input type="checkbox" name="warning" checked onclick="handleClick(this);"/>Warnings<br>
<input type="checkbox" name="info" checked onclick="handleClick(this);"/>Infos<br>
<input type="checkbox" name="minor" checked onclick="handleClick(this);"/>Minors<br>
</div>
<table>
<tr style="border-bottom:1px solid #ccc; height:30px;">
<td style="padding-left:30px; padding-bottom:20px; font-variant:small-caps; font-weight:bold; font-family:Georgia;">Type</td><td style="padding-left:30px;font-variant:small-caps; font-weight:bold; font-family:Georgia; padding-bottom:20px">Message</td></tr>
<?php

 //setcookie("accesslog", md5("sweetbartcaverna"), time()+3600*24*365);  /* expire in 365 days */
 
 //if (isset($_COOKIE['accesslog']) && $_COOKIE['accesslog'] == md5("sweetbartcaverna") )
 //{
    $link = "/home/ubuntu/debianadmin/logs/".file_get_contents("/home/ubuntu/debianadmin/upload/log_position.txt");
    echo nl2br(file_get_contents($link));
 //}
// else
 //{
 //   echo "Access denied. This intrusion attempt will be reported.";
// }
?>
</table>
</body>
</html>