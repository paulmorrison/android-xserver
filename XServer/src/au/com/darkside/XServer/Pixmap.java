/**
 * This class implements an X pixmap.
 */
package au.com.darkside.XServer;

import java.io.IOException;



/**
 * @author Matthew Kwan
 * 
 * This class implements an X pixmap.
 */
public class Pixmap extends Resource {
	private final Drawable		_drawable;
	public final ScreenView			_screen;

	/**
	 * Constructor.
	 *
	 * @param id	The pixmap's ID.
	 * @param xServer	The X server.
	 * @param client	The client issuing the request.
	 * @param screen	The screen.
	 * @param width	The pixmap width.
	 * @param height	The pixmap height.
	 * @param depth	The pixmap depth.
	 */
	public Pixmap (
		int			id,
		XServer		xServer,
		Client		client,
		ScreenView	screen,
		int			width,
		int			height,
		int			depth
	) {
		super (PIXMAP, id, xServer, client);

		_drawable = new Drawable (width, height, depth, null, 0xff000000);
		_screen = screen;
	}

	/**
	 * Return the pixmap's screen.
	 *
	 * @return	The pixmap's screen.
	 */
	public ScreenView
	getScreen () {
		return _screen;
	}

	/**
	 * Return the pixmap's drawable.
	 *
	 * @return	The pixmap's drawable.
	 */
	public Drawable
	getDrawable () {
		return _drawable;
	}

	/**
	 * Return the pixmap's depth.
	 *
	 * @return	The pixmap's depth.
	 */
	public int
	getDepth () {
		return _drawable.getDepth ();
	}

	/**
	 * Process an X request relating to this pixmap.
	 *
	 * @param client	The remote client.
	 * @param opcode	The request's opcode.
	 * @param arg		Optional first argument.
	 * @param bytesRemaining	Bytes yet to be read in the request.
	 * @throws IOException
	 */
	@Override
	public void
	processRequest (
		Client			client,
		byte			opcode,
		int				arg,
		int				bytesRemaining
	) throws IOException {
		InputOutput		io = client.getInputOutput ();

		switch (opcode) {
			case RequestCode.FreePixmap:
				if (bytesRemaining != 0) {
					io.readSkip (bytesRemaining);
					ErrorCode.write (client, ErrorCode.Length, opcode, 0);
				} else {
					_xServer.freeResource (_id);
					if (_client != null)
						_client.freeResource (this);
				}
				break;
			case RequestCode.GetGeometry:
				if (bytesRemaining != 0) {
					io.readSkip (bytesRemaining);
					ErrorCode.write (client, ErrorCode.Length, opcode, 0);
				} else {
					writeGeometry (client);
				}
				break;
			case RequestCode.CopyArea:
			case RequestCode.CopyPlane:
			case RequestCode.PolyPoint:
			case RequestCode.PolyLine:
			case RequestCode.PolySegment:
			case RequestCode.PolyRectangle:
			case RequestCode.PolyArc:
			case RequestCode.FillPoly:
			case RequestCode.PolyFillRectangle:
			case RequestCode.PolyFillArc:
			case RequestCode.PutImage:
			case RequestCode.GetImage:
			case RequestCode.PolyText8:
			case RequestCode.PolyText16:
			case RequestCode.ImageText8:
			case RequestCode.ImageText16:
			case RequestCode.QueryBestSize:
				_drawable.processRequest (_xServer, client, _id, opcode, arg,
															bytesRemaining);
				return;
			default:
				io.readSkip (bytesRemaining);
				bytesRemaining = 0;
				ErrorCode.write (client, ErrorCode.Implementation, opcode, 0);
				break;
		}
	}

	/**
	 * Write details of the pixmap's geometry in response to a GetGeometry
	 * request.
	 *
	 * @param client	The remote client.
	 * @throws IOException
	 */
	private void
	writeGeometry (
		Client			client
	) throws IOException {
		InputOutput		io = client.getInputOutput ();

		synchronized (io) {
			Util.writeReplyHeader (client, 32);
			io.writeInt (0);	// Reply length.
			io.writeInt (_screen.getRootWindow().getId ());	// Root window.
			io.writeShort ((short) 0);	// X.
			io.writeShort ((short) 0);	// Y.
			io.writeShort ((short) _drawable.getWidth ());	// Width.
			io.writeShort ((short) _drawable.getHeight ());	// Height.
			io.writeShort ((short) 0);	// Border width.
			io.writePadBytes (10);	// Unused.
		}
		io.flush ();
	}

	/**
	 * Process a CreatePixmap request.
	 *
	 * @param xServer	The X server.
	 * @param client	The client issuing the request.
	 * @param id	The ID of the pixmap to create.
	 * @param depth		The depth of the pixmap.
	 * @param drawable	The drawable whose depth it must match.
	 * @throws IOException
	 */
	public static void
	processCreatePixmapRequest (
		XServer			xServer,
		Client			client,
		int				id,
		int				depth,
		Resource		drawable
	) throws IOException {
		InputOutput		io = client.getInputOutput ();
		int				width = io.readShort ();	// Width.
		int				height = io.readShort ();	// Height.
		ScreenView		screen;
		Pixmap			p;

		if (drawable.getType () == Resource.PIXMAP)
			screen = ((Pixmap) drawable).getScreen ();
		else
			screen = ((Window) drawable).getScreen ();

		p = new Pixmap (id, xServer, client, screen, width, height, depth);
		xServer.addResource (p);
		client.addResource (p);
	}
}