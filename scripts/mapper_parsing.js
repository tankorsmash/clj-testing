// import esprima from 'esprima-next'
const esprima = require('esprima-next');

const util = require('util');

const fs = require('fs');

// JS to load a weapon frame
// body[5] is what I want to replace with 'find class export'
//var weapon_frame_js = fs.readFileSync("C:\\Users\\Josh\\Documents\\cocos_projects\\buildup_graph\\assets\\js\\main\\magnolia\\frame_mappers\\weaponMapper.js", "utf-8")
// body5 = parsed.body.filter(obj=> obj.type == "ExportNamedDeclaration")[0]
// formfields = esprima.parseModule(weapon_frame_js, {jsx:true, tolerant:true}).body[5].declaration.body.body.filter(obj => obj.key.name == "_frameFormFields")[0]['value'].elements
//esprima.parseModule(weapon_frame_js, {jsx:true, tolerant:true}).body[5].declaration.body.body.filter(obj => obj.key.name == "_frameFormFields")[0]['value'].elements

// let weapon_frame_js = fs.readFileSync("C:\\Users\\Josh\\Documents\\cocos_projects\\buildup_graph\\assets\\js\\main\\magnolia\\frame_mappers\\weaponMapper.js", "utf-8")
// let parsed = esprima.parseModule(weapon_frame_js, {jsx:true, tolerant:true});
// let body5 = parsed.body.filter(obj=> obj.type == "ExportNamedDeclaration")[0]
// let formfields = esprima.parseModule(weapon_frame_js, {jsx:true, tolerant:true}).body[5].declaration.body.body.filter(obj => obj.key.name == "_frameFormFields")[0]['value'].elements

//example
// let prop = formfields[1].properties //skip formfields[0] because its bogus, idk
// let result = {}
// prop.map(el => {Object.assign(result, {[el.key.name]: el.value.value})})

// let props = formfields.splice(1, formfields.length)

const parse_mapper = (mapper_filename) => {
    let weapon_frame_js = fs.readFileSync(mapper_filename, "utf-8")
    let parsed = esprima.parseModule(weapon_frame_js, {jsx:true, tolerant:true});
    let body = parsed.body.filter(obj=> obj.type == "ExportNamedDeclaration")[0]
    let form_fields = body.declaration.body.body.filter(obj => obj.key.name == "_frameFormFields")[0]['value'].elements

    return form_fields;
}


const parse_fields = (props) => {
    let results = []
    props.forEach(prop => {
        let result = {};
        prop.properties.map(el => {
            if (el.value.type == "Literal" || el.value.type == "Identifier") {
                Object.assign(result, {[el.key.name]: el.value.value})
            } else {
                Object.assign(result, {[el.key.name] : el.value.elements.map(el => parse_fields([el]))});
            }
        })

        results.push(result);
    });
    return results;
}

// let raw_form_fields = parse_mapper("C:\\Users\\Josh\\Documents\\cocos_projects\\buildup_graph\\assets\\js\\main\\magnolia\\frame_mappers\\weaponMapper.js");
//
// let toplevel_fields = parse_fields(raw_form_fields.splice(1, raw_form_fields.length));

const parse_mapper_relative = (mapper_name) => {
    let raw_form_fields = parse_mapper(`C:\\Users\\Josh\\Documents\\cocos_projects\\buildup_graph\\assets\\js\\main\\magnolia\\frame_mappers\\${mapper_name}`);

    let toplevel_fields = parse_fields(raw_form_fields.splice(1, raw_form_fields.length));

    return toplevel_fields;
}

// return parse_mapper_relative("weaponMapper.js");
let cli_args = process.argv.slice(2);

let mappers_to_use = [];
if (cli_args.some(el => el == "--all")) {
    mappers_to_use.push(...[
        "weaponMapper.js",
        "actorMapper.js",
        "armorMapper.js",
        "attributeMapper.js",
        "battleTextStructMapper.js",
        "encounterMapper.js",
        "eventChoiceMapper.js",
        "eventOutcomeMapper.js",
        "locationMapper.js",
        "resultTextStructMapper.js",
        "spellMapper.js",
        "standardCombatEventMapper.js",
        "weaponCategoryMapper.js",
        "weaponMapper.js",
        "zoneMapper.js",
    ]);
} else {
    cli_args.forEach((val, idx, arr) => {
        if (val != "--all") {
            mappers_to_use.push(val);
        }
    })
}
if (cli_args.length == 0) {
    console.log("pass relative names of the frame mappers, ie weaponMapper.js\nOR pass --all to use the premade list, and ignore the specified args");
} else {
    mappers_to_use.forEach((val, idx, arr) => {
        let parsed = parse_mapper_relative(val);
        let keyed = {[val] : parsed};
        // console.log(keyed); //simple formatted log
        // console.log(util.inspect(keyed, {depth:null, colors:true})); //expanded formatted log
        console.log(JSON.stringify(keyed)); //dump json
    });
}
