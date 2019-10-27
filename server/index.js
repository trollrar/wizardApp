const express = require("express")                                                                                                                                                                               

const app = express()
const port = 9000

let lastSpell = null;

app.get('/', (req, res) => {
	console.log(req.query);
	if(lastSpell != null && lastSpell.caster != req.query.key) {
		res.send(lastSpell.name);
		lastSpell = null;
	} else {
		res.send("");
	}
});

app.post('/', (req, res) => {
	lastSpell = { caster: req.query.key, name: req.query.name };
	res.send("");
});

app.listen(port, () => console.log(`Example app listening on port ${port}!`))
