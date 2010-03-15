/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/
package org.sakaiproject.blog.api.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class JpegTransformer
{
	BufferedImage originalImage;

	Double imageHeight;

	Double imageWidth;

	private ImageWriter imageWriter;

	public JpegTransformer(byte[] bytes) throws JpegTransformerException
	{
		try
		{
			this.originalImage = ImageIO.read(new ByteArrayInputStream(bytes));
			imageHeight = new Double(this.originalImage.getHeight());
			imageWidth = new Double(this.originalImage.getWidth());
			
			Iterator i = ImageIO.getImageWritersByMIMEType("image/jpeg");

            if (!i.hasNext())
            {
            	throw new Exception("The selected image writer cannot encode jpegs ...");
            }
            
            imageWriter = (ImageWriter) i.next();
            
            if(!imageWriter.getDefaultWriteParam().canWriteCompressed())
            {
            	throw new Exception("The selected image writer cannot compress !");
            }
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.JpegFileTransformer()}[IOException]" + e.getMessage());
		}
	}

	public JpegTransformer(File originalFile) throws JpegTransformerException
	{
		try
		{
			originalImage = ImageIO.read(originalFile);
			imageHeight = new Double(originalImage.getHeight());
			imageWidth = new Double(originalImage.getWidth());
			
			Iterator i = ImageIO.getImageWritersByMIMEType("image/jpeg");

            if (!i.hasNext())
            {
            	throw new Exception("The selected image writer cannot encode jpegs ...");
            }
            
            imageWriter = (ImageWriter) i.next();
            
            if(!imageWriter.getDefaultWriteParam().canWriteCompressed())
            {
            	throw new Exception("The selected image writer cannot compress !");
            }

		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.JpegFileTransformer()}[IOException]" + e.getMessage());
		}
	}

	public float getLongestDimension()
	{
		if (imageWidth.floatValue() > imageHeight.floatValue()) return imageWidth.floatValue();
		return imageHeight.floatValue();

	}

	public float getWidth()
	{
		return imageWidth.floatValue();
	}

	public float getHeight()
	{
		return imageHeight.floatValue();
	}

	/**
	 * Takes a Jpeg file transforming it in a new one using a scale factor and compression quality The size of the new image will be the original width multiplied by the scale factor. The same is
	 * applied to image's height; The compression quality has to be between 1 and 0, being 1 the highest quality and 0 the lowest.
	 */
	public void transformJpeg(File targetFile, float scaleFactor, float quality) throws JpegTransformerException
	{
		try
		{
			Float height = new Float(originalImage.getHeight());
			Float width = new Float(originalImage.getWidth());

			int scaleWidth = new Float(width.floatValue() * scaleFactor).intValue();
			int heightScale = new Float(height.floatValue() * scaleFactor).intValue();

			transformJpeg(targetFile, scaleWidth, heightScale, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFile()}[Exception]" + e.getMessage());
		}
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the width or height (the bigest one), keeping the proportions and using a compression queality The compression quality has to be between 1
	 * and 0, being 1 the highest quality and 0 the lowest.
	 */
	public void transformJpegFixingLongestDimension(File targetFile, int longestDimension, float quality) throws JpegTransformerException
	{
		if (imageHeight.doubleValue() > imageWidth.doubleValue())
			transformJpegFixingHeight(targetFile, longestDimension, quality);
		else
			transformJpegFixingWidth(targetFile, longestDimension, quality);
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the width or height (the bigest one), keeping the proportions and using a compression queality The compression quality has to be between 1
	 * and 0, being 1 the highest quality and 0 the lowest.
	 */
	public byte[] transformJpegFixingLongestDimension(int longestDimension, float quality) throws JpegTransformerException
	{
		if (imageHeight.doubleValue() > imageWidth.doubleValue())
			return transformJpegFixingHeight(longestDimension, quality);
		else
			return transformJpegFixingWidth(longestDimension, quality);
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the width, keeping the proportions and using a compression queality The compression quality has to be between 1 and 0, being 1 the highest
	 * quality and 0 the lowest.
	 */
	public void transformJpegFixingWidth(File targetFile, int fixedWidth, float quality) throws JpegTransformerException
	{
		try
		{
			float scaleFactor = fixedWidth / imageWidth.floatValue();

			int scaleWidth = new Float(imageWidth.floatValue() * scaleFactor).intValue();
			int scaleHeight = new Float(imageHeight.floatValue() * scaleFactor).intValue();

			transformJpeg(targetFile, scaleWidth, scaleHeight, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFileFixingWidth()}[Exception]" + e.getMessage());
		}
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the width, keeping the proportions and using a compression queality The compression quality has to be between 1 and 0, being 1 the highest
	 * quality and 0 the lowest.
	 */
	public byte[] transformJpegFixingWidth(int fixedWidth, float quality) throws JpegTransformerException
	{
		try
		{
			float scaleFactor = fixedWidth / imageWidth.floatValue();

			int scaleWidth = new Float(imageWidth.floatValue() * scaleFactor).intValue();
			int scaleHeight = new Float(imageHeight.floatValue() * scaleFactor).intValue();

			return transformJpegImage(scaleWidth, scaleHeight, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFileFixingWidth()}[Exception]" + e.getMessage());
		}
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the height, keeping the proportions and using a compression queality The compression quality has to be between 1 and 0, being 1 the highest
	 * quality and 0 the lowest.
	 */
	public void transformJpegFixingHeight(File targetFile, int fixedHeight, float quality) throws JpegTransformerException
	{
		try
		{

			float scaleFactor = fixedHeight / imageHeight.floatValue();

			int scaleWidth = new Float(imageWidth.floatValue() * scaleFactor).intValue();
			int scaleHeight = new Float(imageHeight.floatValue() * scaleFactor).intValue();

			transformJpeg(targetFile, scaleWidth, scaleHeight, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFileFixingHeight()}[Exception]" + e.getMessage());
		}
	}

	/**
	 * Takes a Jpeg file transforming it in a new one fixing the height, keeping the proportions and using a compression queality The compression quality has to be between 1 and 0, being 1 the highest
	 * quality and 0 the lowest.
	 */
	public byte[] transformJpegFixingHeight(int fixedHeight, float quality) throws JpegTransformerException
	{
		try
		{

			float scaleFactor = fixedHeight / imageHeight.floatValue();

			int scaleWidth = new Float(imageWidth.floatValue() * scaleFactor).intValue();
			int scaleHeight = new Float(imageHeight.floatValue() * scaleFactor).intValue();

			return transformJpegImage(scaleWidth, scaleHeight, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFileFixingHeight()}[Exception]" + e.getMessage());
		}
	}

	/**
	 * Takes a Jpeg file transforming it to adjust to a width and height and compression quality The compression quality has to be between 1 and 0, being 1 the highest quality and 0 the lowest.
	 */
	public void transformJpeg(File targetFile, int width, int height, float quality) throws JpegTransformerException
	{
		try
		{
			transformJpegImage(targetFile, width, height, quality);
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegFile()}[Exception]" + e.getMessage());
		}
	}

	public byte[] transformJpegImage(int width, int height, float quality) throws JpegTransformerException
	{
		try
		{
			BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			Graphics2D graphics2D = newImage.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(originalImage, 0, 0, width, height, null);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageOutputStream out = new MemoryCacheImageOutputStream(baos);
			imageWriter.setOutput(out);
			ImageWriteParam param = imageWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);
			
			imageWriter.write(null,new IIOImage(newImage,null,null),param);

			baos.flush();
			byte[] bytes = baos.toByteArray();
			out.close();
			
			return bytes;
		}
		catch (FileNotFoundException e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[FileNotFoundException]" + e.getMessage());

		}
		catch (IOException e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[IOException]" + e.getMessage());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[Exception]" + e.getMessage());
		}
	}

	private void transformJpegImage(File targetFile, int width, int height, float quality) throws JpegTransformerException
	{
		try
		{
			BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			Graphics2D graphics2D = newImage.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(originalImage, 0, 0, width, height, null);

			ImageOutputStream out = new FileImageOutputStream(targetFile);
			imageWriter.setOutput(out);
			ImageWriteParam param = imageWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);
			
			imageWriter.write(null,new IIOImage(newImage,null,null),param);
			
			out.close();
		}
		catch (FileNotFoundException e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[FileNotFoundException]" + e.getMessage());

		}
		catch (IOException e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[IOException]" + e.getMessage());
		}
		catch (Exception e)
		{
			throw new JpegTransformerException("\n{JpegTransformer.transformJpegImage()}[Exception]" + e.getMessage());
		}
	}
}
