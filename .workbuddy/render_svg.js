const { Resvg } = require('@resvg/resvg-js');
const fs = require('fs');
const [,, input, output, width] = process.argv;
const svg = fs.readFileSync(input, 'utf8');
const resvg = new Resvg(svg, {
  fitTo: { mode: 'width', value: parseInt(width || '2000', 10) },
  loadSystemFonts: true,
  background: 'white'
});
fs.writeFileSync(output, resvg.render().asPng());
console.log('rendered:', output);
