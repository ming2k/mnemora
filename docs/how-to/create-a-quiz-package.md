# How to Create and Package a Question Bank

A question bank in Mnemora is a ZIP archive (with extension `.zip`, `.quizpkg`, or `.mnemorapkg`) that contains a `data.json` file and any referenced assets such as images. This guide covers the directory layout, the JSON schema, image handling, and the helper script that validates and bundles everything.

For the rationale behind the package format, see [ADR-0004: Multiple Debug Sample Packages](../adr/0004-multiple-debug-sample-packages.md).

## When to use this

Use this guide when you want to:

- Import a custom set of questions into Mnemora.
- Bundle images, diagrams, or charts alongside question text.
- Organise questions into an arbitrarily deep hierarchy of nodes.
- Validate a question bank before sharing or distributing it.

## Prerequisites

- Python 3.8+ (for the helper script)
- A text editor or IDE for writing `data.json`
- Image editing tools if you are creating diagrams or charts

## Package Structure

A valid package is a ZIP archive with the following layout:

```
my-quiz.mnemorapkg
├── data.json          ← Required. The question bank manifest.
└── images/            ← Optional. Assets referenced by relative path.
    ├── diagram_01.png
    └── chart_02.jpg
```

Rules:

- `data.json` must exist at the archive root or inside exactly one nested folder. If it is nested one level deep, the app flattens that folder automatically on import.
- Asset paths inside `data.json` are relative to the archive root.
- The app extracts the entire archive to internal storage, so any file type can be bundled as long as it is referenced correctly.

## Step 1: Write `data.json`

`data.json` is the manifest that describes the entire question bank. It is a single JSON object.

### Root object

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | string | No | `"Imported"` | Display name of the book. |
| `description` | string | No | `null` | Optional description or subtitle. |
| `icon` | string | No | `null` | Icon keyword. See [Book icon mapping](#book-icon-mapping). |
| `nodes` | array | No | `[]` | Ordered list of node objects. Nodes can nest arbitrarily via `children`. |

### Node object

A node is a generic container that can hold questions, child nodes, or both. There is **no fixed depth limit in the schema** — you can nest `children` as deep as you need. The app enforces a **runtime maximum depth of 5 levels** to prevent UI degradation.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | No | Node heading. |
| `children` | array | No | Ordered list of child node objects. |
| `questions` | array | No | Ordered list of question objects attached to this node. |

### Question object

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `content` | string | No | `null` | Question text. Supports **Markdown** and **LaTeX** (`$...$` and `$$...$$`). |
| `choices` | array | No | `[]` | Array of choice objects. Required for `multiple_choice` and `true_false`. |
| `answer` | string | No | `null` | Correct answer. For `multiple_choice` and `true_false`, this is the `key` (e.g., `"A"`). For `fill_blank` and `cloze`, it is the answer text. |
| `explanation` | string | No | `null` | Explanation shown after answering. Supports Markdown and LaTeX. |
| `question_type` | string | No | `"multiple_choice"` | One of: `multiple_choice`, `true_false`, `fill_blank`, `cloze`, `flashcard`, `passage`. See [Question types](#question-types). |
| `front_template` | string | No | `null` | Override for flashcard front face. |
| `back_template` | string | No | `null` | Override for flashcard back face. |
| `sub_questions` | array | No | `[]` | Child questions. Only used when `question_type` is `passage`. |

### Choice object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `key` | string | Yes | Option identifier, e.g. `"A"`, `"B"`. |
| `content` | string | Yes | Primary display text. Supports Markdown and LaTeX. |

> Legacy aliases `html` and `text` are accepted as fallbacks for `content`, but `content` is preferred.

### Question types

| Value | UI Behaviour | Choices Required? |
|-------|-------------|-------------------|
| `multiple_choice` | Displays choices as a list. Tapping reveals instant feedback. | Yes |
| `true_false` | Same as `multiple_choice`, but conventionally uses keys `A=True`, `B=False`. | Yes |
| `fill_blank` | Displays a text input field and a submit button. | No |
| `cloze` | Same as `fill_blank`. The expected answer is the full text with all blanks filled in, comma-separated. | No |
| `flashcard` | Displays a reveal button. Tapping shows the `answer` / `back_template`. | No |
| `passage` | Not answerable directly. Renders the passage text and then renders each `sub_question` as a standalone question. | No |

### Minimal example

```json
{
    "name": "My Quiz",
    "description": "A brief description of this question bank.",
    "icon": "school",
    "nodes": [
        {
            "title": "Level 1",
            "children": [
                {
                    "title": "Level 1.1",
                    "questions": [
                        {
                            "content": "What is $2 + 2$?",
                            "choices": [
                                {"key": "A", "content": "3"},
                                {"key": "B", "content": "4"}
                            ],
                            "answer": "B",
                            "explanation": "$2 + 2 = 4$.",
                            "question_type": "multiple_choice"
                        }
                    ]
                }
            ]
        }
    ]
}
```

### Deep nesting example

```json
{
    "name": "Deep Hierarchy Demo",
    "nodes": [
        {
            "title": "Root Node",
            "children": [
                {
                    "title": "Child A",
                    "children": [
                        {
                            "title": "Grandchild A1",
                            "questions": [...]
                        }
                    ]
                },
                {
                    "title": "Child B",
                    "questions": [...]
                }
            ]
        }
    ]
}
```

> The app will reject packages that exceed **5 levels of nesting** to keep the UI performant.

## Step 2: Add images

Images are referenced inside `content`, `explanation`, `choices`, `front_template`, or `back_template` using standard Markdown syntax:

```markdown
![Geometry diagram](images/diagram_01.png)
```

### Image path rules

- Use **relative paths** from the archive root.
- Supported raster formats: **PNG**, **JPEG/JPG**, **WebP**.
- **SVG** is supported as of the `coil-svg` integration. See [ADR-0004](../adr/0004-multiple-debug-sample-packages.md) for the media support evolution.
- Remote images (`http://` or `https://`) are left unchanged and loaded from the network.
- Avoid absolute `file://` URLs inside `data.json`; they will break on other devices.

### Organising assets

Keep all images in a single folder (conventionally `images/`) so paths stay short and predictable:

```
my-quiz.mnemorapkg
├── data.json
└── images/
    ├── triangle.png
    └── chart.jpg
```

## Step 3: Validate and bundle

Mnemora includes a helper script at `scripts/package-quiz.py` that validates the structure and produces the archive.

### Run the script

```bash
python3 scripts/package-quiz.py <source_directory> [output_file]
```

Example:

```bash
python3 scripts/package-quiz.py ~/my-quiz-bank ~/my-quiz-bank.mnemorapkg
```

If you omit the output file, the script creates `<directory_name>.mnemorapkg` next to the source directory.

### What the script checks

| Check | Severity | Result if failed |
|-------|----------|----------------|
| `data.json` exists | Error | Package is not created. |
| Valid JSON | Error | Package is not created. |
| Root is a JSON object | Error | Package is not created. |
| All image references resolve to files | Error | Package is not created. |
| `question_type` values are known | Warning | Package is still created, but the app may treat the question as `unknown`. |

### Manual ZIP creation (alternative)

If you prefer not to use the script, create the archive manually:

```bash
cd ~/my-quiz-bank
zip -r ../my-quiz-bank.mnemorapkg data.json images/
```

Ensure the file extension is `.zip`, `.quizpkg`, or `.mnemorapkg`.

## Step 4: Import into Mnemora

1. Transfer the package to your Android device.
2. Open Mnemora and tap the **Import** button on the home screen.
3. Select the package file.
4. Wait for the import progress to complete.

The app extracts the archive to internal storage and parses `data.json` into the local database. A book card appears on the home screen immediately after a successful import.

## Book icon mapping

The `icon` field is a keyword string mapped to Material Icons. Common values:

| Keyword | Icon |
|---------|------|
| `calculate` | Calculate |
| `science` | Science |
| `school` | School |
| `menu_book` | Menu Book |
| `psychology` | Psychology |

If the keyword is unknown or omitted, the app falls back to the first letter of the book name.

## Verification

After importing, verify the following:

1. The book card appears on the home screen with the correct name and icon.
2. Tapping the book opens the detail screen and shows the node hierarchy.
3. Tapping a node navigates to practice with questions from that node.
4. Images render correctly inside questions and explanations.
5. LaTeX formulas render as formatted math.
6. Sub-questions under a `passage` type appear in order.

## Common issues

| Issue | Cause | Fix |
|-------|-------|-----|
| **"Invalid file type"** | Extension is not `.zip`, `.quizpkg`, or `.mnemorapkg`. | Rename the file to use a supported extension. |
| **"No data.json found"** | The archive is missing `data.json` or it is nested too deeply. | Ensure `data.json` is at the archive root or exactly one folder deep. |
| **Images show as blank or broken** | Image path in Markdown does not match the actual file path in the archive. | Check case sensitivity and relative path. Run the helper script to catch missing files. |
| **Passage sub-questions do not appear** | The field name `questions` was used instead of `sub_questions`. | Use `sub_questions` for child questions inside a `passage`. |
| **Unknown question type behaves like multiple choice** | `question_type` value has a typo or is unsupported. | Use one of the supported values listed in [Question types](#question-types). |
| **"Maximum node depth exceeded"** | The `nodes` hierarchy is deeper than 5 levels. | Flatten your hierarchy or split into multiple books. |
| **Import is slow or the app freezes** | The archive contains an extremely large number of images or very large individual files. | Optimise image sizes before bundling. The app has no hard size limit, but large packages degrade UX. |

## See also

- [Tutorials: Getting Started](../tutorials/01-getting-started.md) — Build and run the app.
- [ADR-0004: Multiple Debug Sample Packages](../adr/0004-multiple-debug-sample-packages.md) — Why the package format was designed this way.
- `app/src/debug/assets/demo-comprehensive.zip` — A reference package in the repository that exercises every question type, image format, and Markdown/LaTeX feature.
