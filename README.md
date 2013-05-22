<a href="http://projectdanube.org/" target="_blank"><img src="http://projectdanube.github.com/xdi2/images/projectdanube_logo.png" align="right"></a>
<img src="http://projectdanube.github.com/xdi2/images/logo64.png"><br>

This is a tool to translate the ([Pixel](https://github.com/kynetx/PolicyLanguage)) personal cloud policy language to XDI link contracts.

This is work in progress. 

### Information

* [Translation](https://github.com/projectdanube/xdi2-pixel/wiki/Translation)
* [Code Example](https://github.com/projectdanube/xdi2-pixel/wiki/Code%20Example)

### How to build

First, you need to build the main [XDI2](http://github.com/projectdanube/xdi2) project.

After that, just run

    mvn clean install

To build all components.

### How to run

    mvn jetty:run

Then access the web interface at

	http://localhost:9110/

### How to build as XDI2 plugin

Run

    mvn clean install package -P xdi2-plugin

### Community

Google Group: http://groups.google.com/group/xdi2
