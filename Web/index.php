<html>
<TITLE>LemonWash</TITLE>
<META NAME="DESCRIPTION" CONTENT="Le lavage de voiture et deux roues qui vient à votre rencontre !">
<META NAME="ABSTRACT" CONTENT="">
<META NAME="KEYWORDS" CONTENT="lavage voiture écologique ">
<META NAME="REVISIT-AFTER" CONTENT="10 Days">
<META NAME="RATING" CONTENT="general">
<link rel="stylesheet" type="text/css" href="css/style.css">
<script type="text/javascript" src="http://ajax.googleapis.com/
ajax/libs/jquery/1.4.2/jquery.min.js"></script>
<script type="text/javascript" src="js/jquery.easing.1.3.js.js"></script>
<script type="text/javascript">
$(function() 
{
$("ul li:first").show();

var ck_username = /^[A-Za-z0-9_]{3,20}$/;
var ck_lastname = /^[A-Za-z0-9_]{3,20}$/;
var ck_phonenumber = /^[0-9.]{10,10}$/;
var ck_email = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i 
var ck_password =  /^[A-Za-z0-9!@#$%^&*()_]{6,20}$/;


$('#email').keyup(function()
{
var email=$(this).val();
if (!ck_email.test(email)) 
{
 $(this).next().show().html("Verifiez votre e-mail");
}
else
{
$(this).next().hide();
$("li").next("li.firstname").slideDown({duration: 'slow',easing: 'easeOutElastic'});
}

});

$('#firstname').keyup(function()
{
var username=$(this).val();

if (!ck_username.test(username)) 
{
 $(this).next().show().html("Min 3 caractères sans espace");
}
else
{
$(this).next().hide();
$("li").next("li.lastname").slideDown({duration: 'slow',easing: 'easeOutElastic'});
}

});

$('#lastname').keyup(function()
{
var lastname=$(this).val();

if (!ck_lastname.test(lastname)) 
{
 $(this).next().show().html("Min 3 caractères sans espace");
}
else
{
$(this).next().hide();
$("li").next("li.phonenumber").slideDown({duration: 'slow',easing: 'easeOutElastic'});
}
});
$('#phonenumber').keyup(function()
{
var phonenumber=$(this).val();

if (!ck_phonenumber.test(phonenumber)) 
{
 $(this).next().show().html("Min 10 chiffres");
}
else
{
$(this).next().hide();
$("li").next("li.password").slideDown({duration: 'slow',easing: 'easeOutElastic'});
}
});

$('#password').keyup(function()
{
var password=$(this).val();
if (!ck_password.test(password)) 
{
 $(this).next().show().html("Min 6 caractères");
}
else
{
$(this).next().hide();
$("li").next("li.submit").slideDown({duration: 'slow',easing: 'easeOutElastic'});
}
});


$('#submit').click(function()
{
var email=$("#email").val();
var firstname=$("#firstname").val();
var lastname=$("#lastname").val();
var password=$("#phonenumber").val();
if(ck_email.test(email) && ck_username.test(username) && ck_password.test(password) )
{
$("#form").show().html("<h1>Formulaire envoyé!</h1>");
}
else
{

}
return false;

});





})
</script>
<body>
<div id="header"></div>
<div id="content"><div id="centre"><div id="formcontent"><form method="post" >
<ul>
<li class="email">
<label>Email: </label><br/> 
<input type="text" name="email" id="email" />
<span class="error"></span>
</li>
<li class="firstname">
<label>Prénom: </label><br/> 
<input type="text" name="name" id="firstname" />
<span class="error"></span>
</li>
<li class="lastname">
<label>Nom: </label><br/> 
<input type="text" name="name" id="lastname" />
<span class="error"></span>
</li>
<li class="phonenumber">
<label>Numéro de téléphone: </label><br/> 
<input type="text" name="name" id="phonenumber" />
<span class="error"></span>
</li>
<li class="password">
<label>Password: </label><br/> 
<input type="password" name="password" id="password" />
<span class="error"></span>
</li>
<li class="submit">
<input type="submit" value=" Register " id='submit'/>
</li>
</ul>
</form></div></div></div>

</body>
</html>