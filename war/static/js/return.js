console.log('IMPORT GALPAY.JS FILE!');

function getPay() {
	
	var pay = {};
}

function postAjax(url, data, sucess, fail) {
	var xhr = new XMLHttpRequest();
	xhr.open('POST', url);
	xhr.onreadystatechange = function() {
		if(xhr.readyState > 3 && xhr.status == 200) {
			success(JSON.parse(xhr.responseText));
		} else {
			fail(xhr);
		}
	};
	
	xhr.setRequestHeader("Accept", "application/json");
    xhr.setRequestHeader("Accept-Language", "ko_KR");
    xhr.setRequestHeader("Authorization", );
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(data);
    return xhr;
}

document.addEventListener("DOMContentLoaded", function() {
	var test = '<%= request.getParameter("SERVICE_ID") %>';
	console.log('serviceId ::' + test );
});
