'use strict';

var HtmlHandler = {
    handleError: (msg) => console.log('handle error', msg),
    handleJSON: (json) => console.log('handle json', json),
    handleHtml: (html, url) => console.log(url, html)
}

var currentUrl = encodeURI('http://kissanime.ru/');

if(document.documentElement === null) {
    HtmlHandler.handleError('null document');
} else if (document.title !== 'Please wait 5 seconds...') {
    if(document.documentElement.innerHTML.length < 150) {
        HtmlHandler.handleError(document.documentElement.innerHTML);
    } else if(document.documentElement.innerHTML.length > 10000) {
        if(document.getElementById('slcQualix') !== null) {
            var qualities = document.getElementById('slcQualix').options;
            var dictionary = {};
            for(var i = 0; i < qualities.length; i++) {
                dictionary[qualities[i].text] = ovelWrap(qualities[i].value);
            }
            HtmlHandler.handleJSON(JSON.stringify(dictionary));
        } else if(document.getElementById('selectServer') !== null) {
            var serverSelector = document.getElementById('selectServer');
            var server = serverSelector.options[serverSelector.selectedIndex].text;
            if(server === 'Openload') {
                var openload = document.getElementById('divContentVideo').getElementsByTagName('iframe')[0].src
                window.location = openload;
            } else {
                HtmlHandler.handleError('No server available');
            }
        } else if(document.getElementById('streamurl') !== null) {
            var id = document.getELementById('streamurl').innerHTML;
            var link = {
                Openload: ('https://openload.co/stream/' + id + '?mime=true')
            };
            HtmlHandler.handleJSON(JSON.stringify(link));
        } else if(window.location.href === currentUrl) {
            HtmlHandler.handleHtml(document.documentElement.innerHTML, window.location.href);
        }
    }
}
