<?php
class sql {
	private $requete;
	private $sortie;
	private $query;
	public $out;
	public $nb;
	public $insert_id;
	private $insert;
	
	private $host;
	private $user;
	private $psw;
	private $bdd;
	
	function __construct($requete,$sortie="array",$insert=0) 		
	{
		global $SERVER;
		
		$this->requete=$requete;
		$this->sortie=$sortie;
		$this->insert=$insert;
		
		$this->host=$SERVER[1]['SERVER'];
		$this->user=$SERVER[1]['USER'];
		$this->psw=$SERVER[1]['PASSWORD'];
		$this->bdd=$SERVER[1]['BASE'];
		

		self::lunch();
	}
	
	private function connect() {
		mysql_connect($this->host, $this->user, $this->psw);
		mysql_select_db($this->bdd);
	}
	
	public function lunch() {
		self::connect();
		$this->query = mysql_query($this->requete) or die('Erreur SQL !<br>'.$this->requete.'<br>'.mysql_error());
		if($this->sortie == "array") {
			$this->nb = 0;
			while($data = mysql_fetch_assoc($this->query)) {
				foreach ($data as $key => $value)
					$this->out[$this->nb][$key] = $value; 
				$this->nb++;
		}
		}else if($this->sortie == "null"){
			if($this->insert == 1)
				$this->insert_id = mysql_insert_id();
		}
		mysql_close();
		return $this->out;
	}
}
/*
$game = new sql("SELECT * FROM batiment","array");
echo "<br> id:".$game->out[1]['id']."<br>";
*/
?>
