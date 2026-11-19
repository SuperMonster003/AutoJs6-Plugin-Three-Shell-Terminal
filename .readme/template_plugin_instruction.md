{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
if (!terminal.isAvailable()) exit();
let result = terminal.exec('uname -m', {
    show: false,
    keepOpen: false,
    wait: true,
});
console.log(result.exitCode);
let session = terminal.open({ show: false });
session.on('output', (text) => console.log(text));
session.write('ls\n');
session.show();
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
