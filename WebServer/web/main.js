document.getElementById('button').addEventListener('click', function() 
{
window.location.href='http://localhost:8080/scoretable';
}
)

var pics = new Array()
pics[0] = "benchpress.png";
pics[1] = "benchpress2.png";
pics[2] = "benchpress3.png";
var x=1;
setTimeout("changePic()", 5000);

function changePic()
{
	document.getElementById("bppic").src=pics[x]
	x++;
	if (x > 2)
	{
		x = 0;
	}
	setTimeout("changePic()", 5000);
}


document.getElementById("button2").addEventListener('click', async function()
{
	var response = await fetch('submitmsg.txt');
	var text = await response.text();
	var s = document.getElementById('quote');
	s.innerHTML = text;
	setTimeout("removeMsg()", 3000);
}
)

function removeMsg()
{
    var s = document.getElementById('quote');
	var text2 = "";
	s.innerHTML = text2;
}
;