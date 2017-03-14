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
        if(document.getElementById('selectQuality') !== null) {
            var qualities = document.getElementById('selectQuality').options;
            var dictionary = {};
            for(var i = 0; i < qualities.length; i++) {
                dictionary[qualities[i].text] = asp.wrap(qualities[i].value);
            }
            HtmlHandler.handleJSON(JSON.stringify(dictionary));
        } else if(window.location.href === currentUrl) {
            HtmlHandler.handleHtml(document.documentElement.innerHTML, window.location.href);
        }
    }
}
