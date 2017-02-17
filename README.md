# Booster's Lab
a Cave Story level editor

# Overview and History
This project began in early 2012 as a replacement for Cave Editor
to complement my then-ongoing Cave Story fangame/rewrite. Certain features of Cave Editor (CE)
didn't meet my needs and I also wanted to have the experience of creating a 2D map editor. 
Also, I couldn't figure out how to build CE.

Since then, the project has grown to include many features not originally present in CE.
It's also been built to accommodate editing certain other projects that use a Cave-Story-Like format.
As a side-effect of 5 years of sporadic and careless development Booster's Lab has many rough edges and forgotten secrets.
Knowing this, please approach the code with an open mind.

# Building/Running
Booster's Lab is included with project files for IntelliJ Idea Community Edition. If you would rather use
a different tool then that's your decision but I probably won't be able to help you with it.
Booster's Lab has no dependencies. The program has only one `main` method, located in `ca.noxid.lab.EditorApp`. To launch it from intelliJ, create
a run configuration that launches that main method. To export a Jar, create an artifact that contains the compiled output of the program.

# Organization
The project's source files are separated into several packages, each grouping approximately similar functional
units of the editor.

- **ca.noxid.uiComponents**
  - A collection of Swing components extended to have slightly different behaviour or, more often than not, textured backgrounds.
- **ca.noxid.lab**
  - Contains the main class, `EditorApp`, and several other classes that haven't been given a more specific category.
  - **ca.noxid.lab.entity**
    - Classes relating to the Entity view, including the npc.tbl editor
  - **ca.noxid.lab.gameinfo**
    - Classes that manipulate the Cave Story executable, and classes that contain information about the state
      of the game/project
  - **ca.noxid.lab.mapdata**
    - relating specifically to the metadata of each specific map (e.g. which tileset it uses, what its name is, etc)
  - **ca.noxid.lab.rsrc**
    - holds all static resources used by the program such as images and default files, as well as the resource manager class.
  - **ca.noxid.lab.script**
    - Anything to do with TSC (text scripts)
  - **ca.noxid.lab.tile**
    - Classes used by the Tile view, including the tileset pane and linemode behaviour.
    


# Contributing
If you are planning to contribute a change, please open an issue in github's issue tracker so I can let you know
whether it fits into the project roadmap. All merge requests are subject to code review. Please at least approximately
follow standard Java naming and bracket conventions etc. and use tabs for indentation.

# License
This project is licensed under the Apache License, Version 2.0.
http://www.apache.org/licenses/LICENSE-2.0
