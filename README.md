## KSAnime

Android port of a popular anime website.

[160,000+ downloads, 4.5 stars from 7,551 ratings](https://www.appbrain.com/app/ksanime/com.daose.ksanime)

[Youtube Review #1](https://www.youtube.com/watch?v=W-h5qCTBch0)

[YouTube Review #2](https://www.youtube.com/watch?v=eyi5KoziqXI)

- [Background](#background)
- [How it Works](#how-it-works)
- [Documentation](#documentation)
- [DISCLAIMER](#disclaimer)

### Background

KSAnime began as a way for me to learn Android app development while having a project to work on at the same time. However the project suddenly exploded in popularity in August 2017 with 65,724 downloads in one month. I have since unpublished the app as I am not maintaining it and focusing on other projects (plus I am not sure about the legality of scraping other websites).

### How it Works

When the app is launched, a hidden `WebView` is created in the background that connects to the anime website. A `WebView` is used versus a HTML scraping library like `jsoup` because the website is protected under [Cloudflare Advanced DDoS Protection](https://open.spotify.com/browse/featured), which requires the client to have JavaScript and Cookies enabled to access the website.

After the page has been loaded, JavaScript is injected into the page which fetches the HTML and passes the data to Java. Next, `jsoup` is used to scrape the HTML and extract the data that I need.

The data is then transformed and presented to the user as native Android UI, making the experience feel much smoother then relying on a `WebView`. This also allows me to add features that improves the UX (easier shortcuts, ability to add favourites, push notification system, etc.)

### Documentation

The code is not as clean as I would have liked, partly due to the fact that I was still learning Android while developing this app, but also because of the fact that the website I was scraping was under cloudflare's protection, which requires hacky solutions to bypass.

After several refactors, the code is a bit more readable. Below are notable files that demonstrate how I tried to write clean code.

`/api/ka/KA.java`: This class aims to abstract the scraping from the UI code. It acts as the middleman that fetches the HTML from the `WebView` and transforms it to a structured JSON. It exposes methods that allows the UI to hook listeners to and update the UI accordingly when the data comes in.

`model/*.java`: These are [Realm](https://realm.io) models which are stored in the Realm database. One of the advantages of having a local database is that the user can see cached data while new data can be fetched in the background. It makes the UX much better versus a `WebView` where the page is blocked while new data is being fetched.

`web/CustomWebClient.java`: The `WebView` that scrapes the anime site uses a custom web client. This allows me to speed up the page load tremendously by refusing to download CSS files, images, advertisements, etc. when scraping the website. This is also where JavaScript is injected when a page load completes.

`web/HtmlHandler.java`: This acts as the bridge between JavaScript code and Java code. After JavaScript is injected, it is allowed to call methods exposed in this class.

`service/KAService.java`: One of the benefits of a native mobile app (versus a website on a `WebView`), is that you get access to the full suite of Android APIs. One example is this `KAService` class, which allows me to add push notifications to the app to inform the user when anything interesting happens to their starred items.

### DISCLAIMER

>This app is only a themed web browser that browses public websites hosted on third-party servers which are freely available for all internet users. Any legal issues should be taken up with the actual file hosts, as this app is not affiliated with them. Copyrights and trademarks and other promotional materials are held by their respective owners and their use is allowed under the fair use clause of the Copyright Law.
