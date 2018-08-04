'use strict';

var HtmlHandler = {
  handleError: (msg) => console.log('handle error', msg),
  handleJSON: (json) => console.log('handle json', json),
  handleHtml: (html, url) => console.log(url, html)
};

var currentUrl = encodeURI('http://kissanime.ru/');

if(document.documentElement === null) {
  HtmlHandler.handleError('null document');
} else if (document.title !== 'Please wait 5 seconds...') {
  if(document.documentElement.innerHTML.length < 150) {
    //HtmlHandler.handleError(document.documentElement.innerHTML);
  } else if(window.location.host.indexOf('rapidvideo') !== -1) {
    var rapidVideoV2 = document.getElementsByTagName('VIDEO')[0];
    if(rapidVideoV2) {
      HtmlHandler.handleJSON(JSON.stringify({
        RapidVideo: rapidVideoV2.currentSrc
      }));
    } else {
      HtmlHandler.handleError('RapidVideo failed')
    }
  } else if(document.documentElement.innerHTML.length > 10000) {
    if(~window.location.href.indexOf('AreYouHuman')) {
      HtmlHandler.handleError('captcha');
    } else if(document.getElementById('slcQualix') !== null) {
      var qualities = document.getElementById('slcQualix').options;
      var dictionary = {};
      for(var i = 0; i < qualities.length; i++) {
        dictionary[qualities[i].text] = ovelWrap(qualities[i].value);
      }
      HtmlHandler.handleJSON(JSON.stringify(dictionary));
    } else if(document.getElementById('selectServer') !== null) {
      var serverSelector = document.getElementById('selectServer');
      var server = serverSelector.options[serverSelector.selectedIndex].text;
      if(server === 'RapidVideo') {
        var iframes = document.getElementById('divContentVideo').getElementsByTagName('iframe');
        for(var i = 0; i < iframes.length; i++) {
          link = iframes[i].src;
          if (link.indexOf('rapidvideo') !== -1) {
            HtmlHandler.handleJSON(JSON.stringify({RapidVideo: link}));
            break;
          }
        }
      }
      if(server === 'Openload') {
        var redirect = document.getElementById('divContentVideo').getElementsByTagName('iframe')[0].src
        window.location = redirect;
      } else {
        HtmlHandler.handleError('No server available');
      }
    } else if(document.getElementById('streamurl') !== null) {
      var id = document.getElementById('streamurl').innerHTML;
      var link = {
        Openload: ('https://openload.co/stream/' + id + '?mime=true')
      };
      HtmlHandler.handleJSON(JSON.stringify(link));
    } else if(window.location.href === currentUrl) {
      HtmlHandler.handleHtml(document.documentElement.innerHTML, window.location.href);
    }
  } else {
    //HtmlHandler.handleError(document.documentElement.innerHTML);
  }
}
