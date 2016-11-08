Cell_Map : QWindow {
	// la carte des générateurs et de leurs relations
	var map;
	// les dimensions de la carte
	var xSize, ySize;
	// la position actuelle
	var xPos, yPos, zPos, phi;

	*new {|genMap|
		^super.new("Cell Grid", Rect(64, 64, 512, 512), border: true).init(genMap);
	}

	init {|genMap|
		map = genMap;
		xSize = map.size;
		ySize = map[0].size;

		xPos = 0;
		yPos = 0;
		zPos = 1;
		phi = 0.5pi;

		this.background = Color.black;

		this.drawFunc = {|window|
			var drawSize, drawXOffset, drawYOffset;
			var xRange, yRange, yMapSize;

			drawSize = min(window.bounds.width, window.bounds.height);
			drawXOffset = (window.bounds.width - drawSize) / 2;
			drawYOffset = (window.bounds.height - drawSize) / 2;
			Pen.translate(drawXOffset, drawYOffset);
			Pen.scale(drawSize/(zPos*2), drawSize/(zPos*2));

			Pen.color = Color.white;
			Pen.strokeRect(Rect(0,0,zPos*2,zPos*2));
			Pen.addRect(Rect(0,0,zPos*2,zPos*2));
			Pen.clip;

			Pen.use({
				Pen.translate(((xPos-zPos+0.5)%1).neg, ((yPos-zPos+0.5)%1).neg);

				xRange = ((xPos+0.5-zPos).floor..(xPos-0.5+zPos).ceil);
				yRange = ((yPos+0.5-zPos).floor..(yPos-0.5+zPos).ceil);
				yMapSize = yRange.size;

				xRange.do({|cellX, mapX|
					yRange.do({|cellY, y|
						var cell = map[cellX%xSize][cellY%xSize];
						var red, green, blue;
						var mapY = yMapSize - 1 - y;

						Pen.color = Color.grey;
						if((cell[0] & 1) != 0,
							{
								var dir = cell[1][1];
								var dx = dir.fold2(1);
								var dy = (dir-1).fold2(1);
								var start = (mapX+0.5+(0.25*dx))@(mapY+0.5+(0.25*dy));
								var end = (mapX+0.5+(0.75*dx))@(mapY+0.5+(0.75*dy));
								Pen.line(start, end);
								Pen.stroke;
								Pen.addWedge(start, 0.1, ((dir-1.25)%4) * 0.5pi, 0.25pi);
								Pen.fill;
								red = 0.75;
							},
							{red = 0.25});
						if((cell[0] & 4) != 0,
							{
								var dir = cell[1][2];
								var dx = dir.fold2(1);
								var dy = (dir-1).fold2(1);
								var start = (mapX+0.5+(0.25*dx))@(mapY+0.5+(0.25*dy));
								var end = (mapX+0.5+(0.75*dx))@(mapY+0.5+(0.75*dy));
								Pen.line(start, end);
								Pen.stroke;
								Pen.addWedge(start, 0.1, ((dir-1.25)%4) * 0.5pi, 0.25pi);
								Pen.fill;
								green = 0.75
							},
							{green = 0.25});
						if((cell[0] & 2) != 0,
							{
								var dir = cell[1][0];
								var dx = dir.fold2(1);
								var dy = (dir-1).fold2(1);
								var start = (mapX+0.5+(0.25*dx))@(mapY+0.5+(0.25*dy));
								var end = (mapX+0.5+(0.75*dx))@(mapY+0.5+(0.75*dy));
								Pen.line(start, end);
								Pen.stroke;
								Pen.addWedge(start, 0.1, ((dir-1.25)%4) * 0.5pi, 0.25pi);
								Pen.fill;
								blue = 0.75
							},
							{blue = 0.25});
						// Générateurs
						// couleur
						Pen.color = Color(red, green, blue);
						// cercle extérieur
						Pen.strokeOval(Rect(mapX + 0.25,
							mapY + 0.25,
							0.5, 0.5));
						// point central
						if((cell[0] & 16) != 0, {
							Pen.fillOval(Rect(mapX + 0.45,
								mapY + 0.45,
								0.1, 0.1));
						});
						// cercle interieur
						if((cell[0] & 8) != 0, {
							var dir = cell[1][3];
							var dx = dir.fold2(1);
							var dy = (dir-1).fold2(1);
							var start = (mapX+0.5+(0.25*dx))@(mapY+0.5+(0.25*dy));
							var end = (mapX+0.5+(0.75*dx))@(mapY+0.5+(0.75*dy));

							Pen.strokeOval(Rect(mapX + 0.3,
								mapY + 0.3,
								0.4, 0.4));

							Pen.color = Color.grey;
							Pen.line(start, end);
							Pen.stroke;
							Pen.addWedge(start, 0.1, ((dir-1.25)%4) * 0.5pi, 0.25pi);
							Pen.fill;
						});
					});
				});
			});

			Pen.color = Color.white;
			Pen.addWedge((zPos+(0.125*cos(phi)))@(zPos+(0.125*sin(phi).neg)),
				0.25, (0.9pi-phi)%2pi, 0.2pi);
			Pen.fill;
		};

		this.front;
	}

	setPos {|x, y, z, p|
		xPos = x;
		yPos = y;
		zPos = z;
		phi = p;
	}
}