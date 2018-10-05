/**
 * @Author: srehanuddin (main), Added package.json from @atmosuwiryo , Image Printing Function from CesarBalzer
 * @Date:   2018-06-08
 */

var exec = require('cordova/exec');

var BTPrinter = {
	list: function(fnSuccess, fnError) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "list", []);
	},
	connect: function(fnSuccess, fnError, name) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "connect", [name]);
	},
	disconnect: function(fnSuccess, fnError) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "disconnect", []);
	},
	print: function(fnSuccess, fnError, str) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "print", [str]);
	},
	printText: function(fnSuccess, fnError, str) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "printText", [str]);
	},
	printQRCode: function(fnSuccess, fnError, str) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "printQRCode", [str]);
	},
	printImage: function(fnSuccess, fnError, str) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "printImage", [str]);
	},
	printPOSCommand: function(fnSuccess, fnError, str) {
		exec(fnSuccess, fnError, "BluetoothPrinter", "printPOSCommand", [str]);
	}
};

module.exports = BTPrinter;
