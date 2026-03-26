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
   
3. Checkpoint 1 - Plain HTML loads in the browser
   - Create the index file (`resources/public/index.html`)
   - Start the dev server (`npx shawdow-cljs watch app`)
	   - This command kept failing.

Eventually, I discovered at least two errors. First, the [first set of instructions](./reframe-bowling-tutorial) asked me to create `src/myorg/bowling_score/core.cljs` (notice the suffix). The [second set of instructions](./reframe-bowling-tutorial-v2) **does not** have me create this file. Instead, the [second set of instructions](./reframe-bowling-tutorial-v2) only seems to create `cjl_enterprises/bowling_score/core.clj`. (I also appeared to have made a number of directory errors. These, perhaps additional, errors may have also caused the issue.)

When I

- Renamed `cjl_enterprises/bowling_score/core.clj` to `cjl-enterprises/bowling_score/core.cljs`
- Corrected all path references to include both `cjl_enterprises` and `bowling_score`.
  
After 

- Making these changes 
- Executing `npx shadow-cljs watch app`
- Opening `localhost:8080` in my browser

Then I (finally) saw the page that I expected. Sigh...

Continuing with the AI-generated steps...

4. Add ClojureScript and shadow-cljs
	- Added `init` and `on-js-reload` to `core.cljs`
	- Updated `resources/public/index.html` to load the compiled JavaScript at the end of the `body` tag.
5. Checkpoint 2 - "Hello from ClojureScript" in the browser
	- The tutorial reports that I should see the code automatically re-loading, but I must refresh my browser after changing the text in `init`.
	- Additionally, the browser console prints a warning: "shadow-cljs: reloading code but no :after-load hooks are configured". In addition to this message, the link https://shadow-cljs.github.io/docs/UsersGuide.html#_lifecycle_hooks is printed. 
	- I replaced the `on-js-reload` function suggested by Claude with a function, `start`, with the meta-data `^:dev/after-load`. 
	- After this change, I now see the "advertised" behavior: 
		- When I 
			- Change the text in `init` 
			- Save the file, the browser refreshes
		- Then
			- The browser refreshes "automagically"
			- And I see the new text from `init`
	- Additionally, I no longer see the warning about no :after-load hooks
6. Add Reagent - your first component
	- I replaced the contents of `core.cljs` with exactly the code in the tutorial. This replacement code included the call to `on-js-reload` which again is producing a warning.
7. Checkpoint 3 - Reagent renders a heading
	- Grrr. Ran the web search, "Does clojurescript core code call a function named `on-js-reload`". The AI Overview in Chrome reported: "The `on-js-reload` function is a convention used by development tools, particularly the popular build tool **Figwheel**, to provide a hook for managing application state during hot code reloading." **I am not running Figwheel.** I do not recall seeing any references to Figwheel in any part of this tutorial.
	- Replacing the call to `on-js-reload` with a call to `start` with the meta-data `^:dev/after-load` repairs this issue.
	- Additionally, I also see an error that I've seen before "installHook.js:1 Warning: ReactDOM.render is no longer supported in React 18. Use createRoot instead. Until you switch to the new API, your app will behave as if it's running React 17. Learn more: https://reactjs.org/link/switch-to-createroot"
	- To address this issue, I spent way too much time trying to determine the appropriate "incantation" to render a (simple) ClojureScript application handling both
		- Initial loading (`clj-enterprises.bowling-score.core/init`)
		- Re-rendering (`clj-enterprises.bowling-score.core/re-render)
8. Add re-frame - `app-db` and `initialize-db`
	- Create the initial application database (`default-db`) in `db.cljs`
	- Create the `:initialize-db` event handler `rf/reg-event-db` in `events.cljs`
	- Change `core.cljs` to
		- Require `[re-frame.core :as rf`
		- Make `init` synchronously dispatch the `:initialize-db` event
		- Log a message with the contents of the database when initialized. (I discovered that `init` is only called **once** when the page first loads. I believe that `init` is trigged by the call to `rdc/create-root` but I would not be surprised to be wrong.)
9. Checkpoint 4 - app-db initializes without errors
	- Confirmed that I see the correct (one-time) message about `app-db`
	- Confirmed that I see neither errors nor warnings i the console
	- Confirmed the value of the key
		- `rolls`
		- `game-over?`
	- Remove the call to `prn` / `js/console.log` in `core/init` (no longer needed)
		- Famous last words
10. Your first event handler - `:roll-ball` (stub)
	- Add a `:roll-ball` event.
	- The handler for the `:roll-ball` event simply (for now) appends the number of pins knocked down for this roll to the `:roll` item in the application database.
11. Checkpoint 5 - clicking a button updates the db
	- We inspect the database after clicking a button by executing the function
		```clojure
		cljs.core.clj__GT_js(re-frame.db.app_db.state)
		```
		I understand that an arguably better / easier way to accomplish this task is to use the package, `re-frame-10x`.
		
		 In addition, this checkpoint demonstrates the time-honored technique of "`println` debugging" by adding a new HTML paragraph printing out the contents of the `app-db` atom.