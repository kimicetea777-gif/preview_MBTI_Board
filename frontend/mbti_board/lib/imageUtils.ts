export const ALLOWED_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/bmp",
]);

export const ALLOWED_IMAGE_TYPES_LABEL = "JPG·PNG·WebP·BMP";

export const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
export const MAX_IMAGE_BYTES_LABEL = `${MAX_IMAGE_BYTES / (1024 * 1024)}MB`;
