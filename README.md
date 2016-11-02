# SC-CellGrid
SuperCollider extension for the Cell Grid cellular automat

Cell Grid is a SuperCollider extension that generates a cellular automat synthetizer.
It creates a square grid of arbitrary size, composed of cells that interact with one another.
The cells are composed of an oscillator with 4 inputs, taken from the neighbouring cells in random order, which perform (with a certain probability) the following operations:
- Frequency modulation
- Amplitude modulation (Ring modulation)
- Variable delay
- Auxiliary mix in
There follows a chain of effects (each of which is activated according to a certain probability), in a predefined order.
The output of each cell is then fed into the neighbouring cells through a feedback process.
The output of the whole grid is routed to SC's output through the selected output module.

Startup is performed through invocation of Cell_Matrix(args ...). This will potentially reboot the server with extended resources.
Shutdown is performed by calling .free on the resulting object.
