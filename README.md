# Cordova-Plugin-Bluetooth-Printer
A cordova plugin for bluetooth printer for android platform, which support text, image and POS printing.

## Supported Cordova Versions
- cordova: `>= 6`
- cordova-android: `>= 6.4`


## Supports printing:
- Text
- POS Commands
- Image Printing (Thanks to @CesarBalzer)
- **ToDo:** Qrcode Printing (It can be achieved via image for now)
- **ToDo:** Barcode Printing (It can be achieved via image for now)

## Install

```sh
# Using the Cordova CLI and NPM, run:
cordova plugin add https://github.com/pvillaverde/Cordova-Plugin-Bluetooth-Printer

# Using Ionic run:
ionic cordova plugin add https://github.com/pvillaverde/Cordova-Plugin-Bluetooth-Printer
```

## Functions

A notification does have a set of configurable properties. Not all of them are supported across all platforms.

| Method                                                      | Description                                                                          |
| :---------------------------------------------------------- | :----------------------------------------------------------------------------------- |
| BTPrinter.list(fnSuccess, fnError)                          | Returns array of strings with available bluetooth devices names                      |
| BTPrinter.connect(fnSuccess, fnError, name)                 | Connects to the device with the name specified (must be connected via bluetooth)     |
| BTPrinter.disconnect(fnSuccess, fnError)                    | Disconnects from the currently connected device                                      |
| BTPrinter.printText(fnSuccess, fnError, string)             | Prints `string` text. You can use `\n` to break lines.                               |
| BTPrinter.printQRCode(fnSuccess, fnError, data)             | *ToDo:* Prints a QRCode with data as its encoded message                             |
| BTPrinter.printImage(fnSuccess, fnError, base64ImageString) | Prints images with `base64ImageString`. You can print QRCodes && BarCodes this way   |
| BTPrinter.printPOSCommandfn(Success, fnError, string)       | Executes PosCommand specified in `string`. See useful PosCommands below              |

### Important notes:
- You have to send a new line `\n` or feed command `0D` in order to print the last line.
- POS Command `1B 40` restarts all previously set settings via POS Commands.


## Useful POS Commands

These are only some of the POS Commands you can use to customize your printing

| Command       | Description                  | Command       | Description                  |
| :------------ | :--------------------------- | :------------ | :--------------------------- |
| `1B 40`       | Resets and initialize print  | `0D`          | Feeds paper                  |
| `1B 21 00`    | Normal size for text         | `1B 2D 00`    | Underline font off           |
| `1B 21 01`    | Small size for text          | `1B 2D 01`    | Underline font 1-dot ON      |
| `1B 21 10`    | Double height text           | `1B 2D 02`    | Underline font 2-dot ON      |
| `1B 21 20`    | Double width text            | `1B 21 20`    | Double width text            |
| `1B 21 30`    | Double width & height text   | `1B 21 30`    | Double width & height text   |
| `1B 45 00`    | Bold font OFF                | `1B 35`       | Italic font OFF              |
| `1B 45 01`    | Bold font ON                 | `1B 34`       | Italic font ON               |
| `1B 61 00`    | Align Left                   | `1B 4D 00`    | Font type A                  |
| `1B 61 01`    | Align-Center                 | `1B 4D 01`    | Font type B                  |
| `1B 61 02`    | Align-Right                  | `1B 4D 02`    | Font type C                  |

## International Charset

You can specify which charset to use when printing, see [reference](https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=29)
As this is the first setting when printing, you can use this PosCommand `1B 40 1B 52 **code** 1B 74 16`

| Code    | Charset   | Code    | Charset            | Code    | Charset            |
| :------ | :-------- | :------ | :----------------- | :------ | :----------------- |
| `00`    | U.S.A.    | `06`    | Italy              | `12`    | Latin America      |
| `01`    | France    | `07`    | Spain I            | `13`    | Kirea              |
| `02`    | Germany   | `08`    | Japan              | `14`    | Slovenia / Croatia |
| `03`    | U.K.      | `09`    | Norway             | `15`    | China              |
| `04`    | Denmark I | `10`    | Denmark II         | `16`    | Vietnam            |
| `05`    | Sweden    | `11`    | Spain II           | `15`    | Arabia             |



## Example Usage
If using Ionic with AngularJS, you can wrap plugin's function into promises like this:

```js
function checkAvailable() {
	return new Promise((resolve, reject) => {
		$window.BTPrinter.list(success => resolve(success), failure => reject(failure));
	});
}
```
In my case I have done some functions to make it easy to format text:

```js
function style(commands) {
	// https://github.com/humbertopiaia/escpos-commands-js
	const ESCPOS = {
		MARGINS: {
			BOTTOM: `1b 4F`, // Fix bottom size
			LEFT: `1B 6C`, // Fix left size
			RIGHT: `1B 51`, // Fix right size
		},
		TEXT_FORMAT: {
			'size-normal': `1B 21 00`, // Normal text
			'size-small': `1B 21 01`, // Initialize small sized
			'size-2h': `1B 21 10`, // Double height text
			'size-2w': `1B 21 20`, // Double width text
			'size-2x': `1B 21 30`, // Double width & height text

			'underline-off': `1B 2D 00`, // Underline font OFF
			'underline-on': `1B 2D 01`, // Underline font 1-dot ON
			'underline-on-2x': `1B 2D 02`, // Underline font 2-dot ON
			'bold-off': `1B 45 00`, // Bold font OFF
			'bold-on': `1B 45 01`, // Bold font ON
			'italic-off': `1B 35`, // Italic font ON
			'italic-on': `1B 34`, // Italic font ON

			'font-a': `1B 4D 00`, // Font type A
			'font-b': `1B 4D 01`, // Font type B
			'font-c': `1B 4D 02`, // Font type C

			'align-left': `1B 61 00`, // Left justification
			'align-center': `1B 61 01`, // Centering
			'align-right': `1B 61 02`, // Right justification
		},
	};
	let POSCommand = `1B 40`; // Recibe ordenes POS
	//POSCommand += ` 1B 52 11 1B 74 02`;
	commands.split(` `).forEach(cmd => POSCommand += ESCPOS.TEXT_FORMAT[cmd] ? ` ${ESCPOS.TEXT_FORMAT[cmd]}` : ``);
	return printPOSCommand(POSCommand);
}

function setInternationalCharset(country) {
	// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=29
	const charsetCode = {
		'U.S.A.': `00`,
		'France': `01`,
		'Germany': `02`,
		'U.K.': `03`,
		'Denmark I': `04`,
		'Sweden': `05`,
		'Italy': `06`,
		'Spain I': `07`,
		'Japan': `08`,
		'Norway': `09`,
		'Denmark II': `10`,
		'Spain II': `11`,
		'Latin America': `12`,
		'Korea': `13`,
		'Slovenia / Croatia': `14`,
		'China': `15`,
		'Vietnam': `16`,
		'Arabia': `17`,
	};
	let POSCommand = `1B 40 1B 52 ${charsetCode[country]} 1B 74 16`;
	return printPOSCommand(POSCommand);
}
```
And then you can easily print whatever like this:

```js
function printTicket(organization) {
	return printerService.checkAvailable().then(printers => {
		return printerService.connect(printers[0]).then(() => {
			const title = `\n\n${organization.name}\n\n`;
			const lineSeparator = `==============================\n\n`;
			const endSpace = `\n\n\n\n`;
			return printerService.setInternationalCharset(`Spain II`)
				.then(() => printerService.style(`align-center size-2h`))
				.then(() => printerService.printText(title))
				.then(() => printerService.style(`size-normal align-center`))
				.then(() => printerService.printText(`${organization.address.street}\n`))
				.then(() => printerService.printText(`${organization.address.postcode} ${organization.address.city}\n`))
				.then(() => printerService.printText(`${organization.telephone}\n`))
				.then(() => printerService.style(`align-left size-normal`))
				.then(() => printerService.printText(lineSeparator))
				.then(() => printerService.style(`align-center`))
				.then(() => printerService.printImage(qrCodeService.generate(organization.webPage).toDataURL()))
				.then(() => printerService.printText(endSpace));
		});
	}).catch(error => {
		console.error(error);
		printerService.disconnect();
		// Attempt to invoke virtual method 'void java.io.OutputStream.write(byte[])' on a null object reference => Printer not connected
		// read failed, socket might closed or timeout, read ret: -1 => Printer is offline or have been used and didn't disconnect before.
		// invalidAction => Function called with BTPrinter does not exist.
	});
}
```
