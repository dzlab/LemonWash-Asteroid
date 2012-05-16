<?php

function getTimeToH($prefixe,$time)
{
	$out = 0;
	$dt = time() - $time;
	
	if($time == 0){
		$out = "NEVER";
		$prefixe = "";
	}
	elseif($dt > 60*60*24*30.5)
		$out = round($dt/(60*60*34*30.5)) ." mois";
	elseif($dt > 60*60*24)
		$out = round($dt/(60*60*24)) ." jours";	
	elseif($dt > 60*60)
		$out = round($dt/(60*60)) ." heures";
	elseif($dt > 60)
		$out = round($dt/60) ." minutes";
	elseif($dt < 60)
		$out = round($dt) ." secondes";

	return $prefixe.' '.$out;
}
function secure($var)
{
	return htmlentities(addslashes($var),ENT_COMPAT, 'UTF-8');
}

function coolFormat($str) {

	$str = str_replace("\n","",$str);
	$str = str_replace("\r","",$str);
	$str = str_replace("\r\n","",$str);
	$str = str_replace("\t","",$str);
	
	return $str;
}

?>
