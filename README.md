# Rokuality Server - End to End Automation for Roku, XBox, Playstation, Cable SetTop Boxes, and More!

The Rokuality Server allows you to distribute Roku, XBox, PS4, and Cable SetTop Box end to end tests across multiple devices on your network. The server acts a lightweight web proxy, capturing your device test traffic and distributing the tests to the devices provided in your capabilities. The project goal is to provide a no cost/low cost open source solution for various video streaming platforms that otherwise don't offer an easily automatable solution! Once you have the server setup with all requirements, [pick a language](#next-steps-choose-a-test-language) to write tests in. Additionally, it's possible to manually start a [live session test](#live-session-testing) for Roku and XBox devices, and maintain a remote device library from a consolidated location.

### Your Roku tests in the cloud!

Access the [Rokuality Device Cloud](https://www.rokuality.com/) to run your Roku tests in a CI/CD fashion from anywhere in the world! Get access to all the popular Roku streaming devices for both automated and live device test sessions on the world's first Roku Webdriver device cloud. Your [device portal](https://www.rokuality.com/device-portal-and-site-services) will allow you to review your test history, manage your test teams, review run results, and more! Our [detailed documentation](https://www.rokuality.com/roku-automation) will get you and your team up and running quickly. Start a [free trial](https://www.rokuality.com/plans-pricing) today and get started!

### Getting started: Server Requirements
The Rokuality Server is a Java Jetty servlet container application that requires at least Java 8 to run, and that you have Java available on your PATH. Typically the easiest way to do that is to use the relevant package manager for the Operating System you are running. For example, for MAC users it's easy using [brew](https://brew.sh/) via `brew cask install java` or for Windows users using [scoop](https://scoop.sh/) via 
```xml
    scoop bucket add java
    scoop install openjdk10
```

If you have it setup correctly you should be able to run the following command and get a relevant version back:
```xml
    java -version
```
should return something like:

```xml
    java version "1.8.0_221"
    Java(TM) SE Runtime Environment (build 1.8.0_221-b11)
    Java HotSpot(TM) 64-Bit Server VM (build 25.221-b11, mixed mode)
```

### Getting started: Server OCR Requirements
The server uses Tesseract as the default OCR module, so tesseract must be installed and available on your system's PATH. Tesseract is most easily installed on MAC via `brew install tesseract` or on Windows via `scoop install tesseract`

### Getting started: OS Requirements
The Rokuality Server should work on MacOS X 10.11 or higher, Windows 10 or Windows Server (2012 or higher). It has not been tested yet for linux but 'should' work.

### Getting started: Running the Server via Maven
An easy way to start the server is to clone the repository, cd into the main directory, and then compile on the fly. Note that with this approach you will always be running the latest master build, but if you prefer to run a static release version, see [the jar](#getting-started-running-the-server-jar) instructions below. If you don't have maven it can be installed via your relevant Operating System package manager. i.e for MAC via `brew install maven` Or for Windows if you're using scoop `scoop install maven`

And once you have Maven installed, start the server by:

```xml
    cd /path/to/where/you/cloned/rokuality-server/
    mvn clean compile -e exec:java
```
By default this will start the server listening at port 7777. Optionally you can provide an alternate port for the server via:
```xml
    mvn clean compile -e exec:java -Dport=yourport
```

### Getting started: Running the Server JAR
Optionally you can go to the [releases page](https://github.com/rokuality/rokuality-server/releases) and download the latest standalone Server JAR. Once downloaded you can run via:
```xml
    java -Dport=youport -jar rokuality-server.jar
```

Upon a successful server launch you should see a pop indicating that the server is ready and listening. If it's the first time you are launching the server it will take about a minute while it downloads and installs the necessary tesseract trained data files in the background.

### Getting started: Roku
Automated testing on Roku requires the following:
1. Developer mode enabled on your device. [Enabling developer mode](https://blog.roku.com/developer/developer-setup-guide) on your Roku device is very straight forward. Keep track of your device username and password as created during the basic walkthrough as you'll need them to pass to your DeviceCapabilities at driver startup. Once you've enabled developer mode on your device you should be able to hit the device console page at http://yourrokudeviceip - Once that's done you are all set for automation!
2. Go installed and available on your path. The Rokuality project is one of the first frameworks to provide support for [Roku Webdriver](https://github.com/rokudev/automated-channel-testing). Go can be installed on Mac via `brew install go` or on Windows via `scoop install go`.

Optional - If you wish to test a production channel then you must have an HDMI to USB capture card attached to the device and the machine running the Rokuality Server. Once connected the server can capture the device input stream and then perform evaluations against the captured frames. See [why capture cards](#why-hdmi-to-usb-capture-cards) for details.

### Getting started: XBox
Automated testing on XBox requires the following:
1. Google Chrome browser installed on the machine running the Rokuality server. The server uses headless chrome to handle various tasks like installing/launching/uninstalling the XBox appxbundles. You won't physically see chrome launch as it will run in headless mode.
2. Your XBox must be in dev kit mode. [Enabling developer mode](https://docs.microsoft.com/en-us/windows/uwp/xbox-apps/devkit-activation) on your XBox is straight forward but it does require a 19$ Microsoft developer account which will allow you to automate 3 boxes from a single dev account.
3. You must have the [XBox dev console](https://docs.microsoft.com/en-us/windows/uwp/xbox-apps/device-portal-xbox) available for remote access with NO username/password set. If properly setup you should be able to access your dev console remotely at `https://yourxboxip:11443`
4. You must have python 3.5 > < 3.8 installed. Python version 3.8.1 has not been succesfully tested yet. 
5. You must have the [openxbox xbox-smartglass-rest server](https://github.com/OpenXbox/xbox-smartglass-rest-python) installed and on your path. Easily done via `pip3 install xbox-smartglass-rest` The smartglass server is used to send remote control input to the XBox console. Our thanks to the team at OpenXBox for their work on this project.

### Getting started: HDMI Connected Devices (Playstation, Cable SetTopBox, AndroidTV, AppleTV, and More)
The Rokuality platform allows you run automated tests on just about any device that has an HDMI out and accepts either Bluetooth or IR commands. This includes Playstations, AppleTV's, AndroidTV's, Cable SetTop Box's, etc. It requires the following software/hardware:
1. You must have nodejs installed on the machine running the server. Easily done on MAC via `brew install node` or on Windows via `scoop install nodejs`.
2. You must have a [Logitech Harmony Hub](https://www.logitech.com/en-us/product/harmony-hub?crid=60) with your device setup as a device and XMPP enabled on the hub. See the sections [why harmony](#why-harmony) and [configuring your harmony](#configuring-your-harmony) for details.
3. You must have an HDMI to USB capture card attached to the device and the machine running the Rokuality Server. Once connected the server can capture the device input stream and then perform evaluations against the captured frames. See [why capture cards](#why-hdmi-to-usb-capture-cards) for details.

### Server command options:
| Command  | Description | Notes |
| ------------- | ------------- | ------------- |
| port | Starts the server on a port provided by the user.  | OPTIONAL - Defaults to listen on port 7777 |
| commandtimeout | Specifies a session cleanup time in seconds for any orphaned sessions i.e. any sessions that were started but haven't received any commands for a specified duration.  | OPTIONAL - Defaults to 60 seconds. |
| threads | The maximum number of allowed threads the server can handle. | OPTIONAL - Defaults to 10

### Next Steps: Choose a test language:
Pick a language binding to begin writing tests:

[Java](https://github.com/rokuality/rokuality-java)

[Python](https://github.com/rokuality/rokuality-python)

JS (coming soon - contact us to get involved)

[C#](https://github.com/rokuality/rokuality-csharp)

### Live Session Testing
For Roku and XBox devices, it's possible to manually access your remote devices and start a live session. In this fashion you can manage a remote library of machines to distribute tests across a wide variety of device platforms for multiple testers. Note that this functionality is still experimental but should be fairly reliable. 

To start a live test session against an XBox or Roku device:

1. Start the Rokuality server.
2. In a browser, navigate to the /tools page url of your locally running server instance, i.e. http://localhost:7777/tools This will open up a tools menu with a link to 'Start a manual session'. Click this link.
3. A dialog will open asking for connection details. Select the platform and enter the Server URL of the Rokuality server you wish to connect to. NOTE - this url can be a remote location but it MUST be accessible from the connecting machine.
4. Enter in the device specific details you wish to connect to and click the 'Connect' button. Within a few seconds a new frame should appear with your remote control options and the captured image of the device screen.

At this point you can manually interact with your remote device under test.

### Why Harmony
Some of our device automation setups require a [logitech harmony](https://www.logitech.com/en-us/product/harmony-hub?crid=60) to drive the user input. The hub is a low cost (60$) device that gives us both IR and Bluetooth capability in an easily available, wireless solution. So we don't need to use an IR blaster or a cabled base solution. And we can scale across additional devices easily. Thanks to the awesome [harmony cli](https://github.com/sushilks/harmonyHubCLI) project, we are able to incorporate a simple cli solution inside of the Rokuality server to remotely drive the harmony and control the devices. Other similar solutions on the market ship you a "magic box" device that requires you to plug in your device under test - which is proprietary and limits the ability to scale with more devices and across multiple developers. Using the harmony approach is cheap, effective, and scalable.

### Configuring Your Harmony
To setup your Harmony hub and prepare it for automating your devices under test:
1. Download the Harmony mobile app for iOS or Android.
2. During setup it will walk you through adding your device under test. Be sure to keep track of what you name your device when you pair it with your Harmony as that will be used later during your tests as your "DeviceName" capability. This will allow the Rokuality server to communicate with your device from your test code and drive it like a real user.
3. Enable XMPP on the hub. In the harmony app go to Settings>>Harmony Setup>>Add/Edit Devices & Activities>>Hub>>Enable XMPP

### Why HDMI to USB Capture Cards
Some of our devices require you to have an HDMI to USB capture card connected to the device and machine running the Rokuality server. This is necessary for devices like the PS4 or cable set top boxes that don't offer an available means of remotely capturing the screen output. Other vendors ship you a proprietary capture card for these scenarios that operates in the same fashion - and their hardware can be expensive! We allow you to bring your own capture card and provide the same functionality as a lower cost alternative. There are several capture card options available and most 'should' work but we've tested the following:
	* [Magewell USB 3.0](https://www.magewell.com/products/usb-capture-hdmi-gen-2) A 300$ premium capture card recommended for high quality video capture and evaluations.
	* [USB 2.0/3.0](https://www.amazon.com/Capture-Broadcast-Streaming-Grabber-Converter/dp/B0779ZJZX3/ref=sr_1_3?keywords=hdmi+usb+capture+cards&qid=1571405168&sr=8-3) A lower cost, $90 alternative to the magewell. Video quality is not as good as the Magewell but under test has worked perfectly well.