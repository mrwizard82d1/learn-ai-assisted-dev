## First round

### First prompt

"You are a senior software engineer with an excellent knowledge of Clojure, ClojureScript, and re-frame. I was just hired at our company and am a competent junior developer familiar with Clojure and ClojureScript. Create a step-by-step tutorial to introduce me to the re-frame package. To be concrete, use the example of scoring a bowling frame and game for a person as the "use case" for this tutorial. Further, start the tutorial by creating a ClojureScript project using shadow-cljs, shadow.edn and deps.edn (if needed). Perhaps use the deps-new tool to create the project initially using the clj tool but guide me step by step to create the necessary programming infrastructure before tutoring me using the "score a bowling game" example."

### First response

Here is the [first markdown file containing the generated code](./reframe-bowling-tutorial.md).

Additionally, here are the [first generated notes](./reframe-bowling-tutorial-notes.md).

## Second round

### Second prompt

"After a quick review, this looks reasonable; however, it seems that I must type in all the code in one go before I see anything.

Can you change the tutorial to have me check my progress in very small steps so that I can discover errors in very small code chunks since I'm learning re-frame and refreshing HTML and CSS?"

This prompt is moving toward test-driven development but is probably not what I would choose in practice.

### Second response

Here is the [second markdown file containing the generated code](./reframe-bowling-tutorial-v2.md).

Additionally, here are the [second generated notes](./reframe-bowling-tutorial-v2-notes.md).

#### Step-by-step notes

1. Pre-requisites and mental model
   - Check that `deps-new` is installed
     - Tried `clj --version` but this only provides the installed version of the CLI tools
     - Executed `clj -Ttools list`
       - This command lists all installed tools. This list included `deps-new`
       
2. Scaffold the project
   - Create the project: `clj -Tnew app :name clj-enterprises/bowling-score`

