let lastSpell = null;

module.exports = function (context, req) {
    
    const res = { body: '' };

    if (req.method == 'GET') {
        console.log('looking for casted spells: ' + req.query.key);
        if (lastSpell != null && lastSpell.caster != req.query.key) {
            res.body = lastSpell.name;
            lastSpell = null;
        }
    } else

        if (req.method == 'POST') {
            console.log('spell casted: ' + req.query.key + ' ' + req.query.name);
            lastSpell = { caster: req.query.key, name: req.query.name };
        } else {
            res.body = "??" + req.method;
        }

    context.done(null, res);  
};
