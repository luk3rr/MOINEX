#!/usr/bin/env python3

# Filename: get_stock_logo.py
# Created on: February 16, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

import sys
import json
import os
import requests
from typing import Optional
from urllib.parse import urlparse
from pathlib import Path
from io import BytesIO
from PIL import Image

APISTEMIC_LOGOS_API_BASE = "https://logos-api.apistemic.com"
USER_AGENT = "MOINEX (https://github.com/luk3rr/MOINEX)"

# Get logos directory from user home
MOINEX_DATA_DIR = Path.home() / ".moinex"
LOGOS_CACHE_DIR = MOINEX_DATA_DIR / "logos"


def extract_domain_from_url(url: str) -> Optional[str]:
    """
    Extract domain from a URL
    
    :param url: Full URL (e.g., "https://www.apple.com")
    :return: Domain without www (e.g., "apple.com") or None if invalid
    """
    try:
        parsed = urlparse(url)
        domain = parsed.netloc
        
        if domain.startswith("www."):
            domain = domain[4:]
        
        return domain if domain else None
    except Exception:
        return None


def get_logo_url_from_apistemic(domain: str) -> Optional[str]:
    """
    Get logo URL for a domain using apistemic logos API
    
    :param domain: Company domain (e.g., "petrobras.com.br" or "apple.com")
    :return: Logo URL or None if not found
    """
    try:
        url = f"{APISTEMIC_LOGOS_API_BASE}/domain:{domain}"
        response = requests.head(
            url,
            headers={"User-Agent": USER_AGENT},
            timeout=5,
            allow_redirects=True
        )
        
        if response.status_code == 200:
            return url
        else:
            return None
    except Exception:
        return None


def get_cached_logo_path(domain: str) -> Optional[str]:
    """
    Check if logo is already cached locally
    
    :param domain: Company domain (e.g., "apple.com")
    :return: Absolute path if cached, None otherwise
    """
    filename = f"{domain}.png"
    filepath = LOGOS_CACHE_DIR / filename
    
    if filepath.exists():
        return str(filepath)
    return None


def download_and_cache_logo(logo_url: str, domain: str) -> Optional[str]:
    """
    Download logo from URL and cache it locally as PNG
    Converts WebP and other formats to PNG for JavaFX compatibility
    
    :param logo_url: URL of the logo to download
    :param domain: Company domain (e.g., "apple.com")
    :return: Absolute path to cached logo or None if download failed
    """
    try:
        LOGOS_CACHE_DIR.mkdir(parents=True, exist_ok=True)
        
        response = requests.get(
            logo_url,
            headers={"User-Agent": USER_AGENT},
            timeout=10
        )
        
        if response.status_code == 200:
            filename = f"{domain}.png"
            filepath = LOGOS_CACHE_DIR / filename
            
            # Convert image to PNG format for JavaFX compatibility
            try:
                image = Image.open(BytesIO(response.content))
                # Convert to RGBA if necessary
                if image.mode != 'RGBA':
                    image = image.convert('RGBA')
                # Save as PNG
                image.save(filepath, 'PNG')
                return str(filepath)
            except Exception:
                # If conversion fails, save as-is
                with open(filepath, 'wb') as f:
                    f.write(response.content)
                return str(filepath)
        else:
            return None
    except Exception:
        return None


def get_logo_path(website: str, download: bool = True) -> Optional[str]:
    """
    Get logo path for a company website
    First checks cache, then downloads if not cached and download=True
    
    :param website: Company website URL (e.g., "https://www.apple.com" or "apple.com")
    :param download: Whether to download and cache if not found locally
    :return: Absolute path to logo file or None if not found
    """
    if not website:
        return None
    
    domain = extract_domain_from_url(website)
    
    if not domain:
        return None
    
    # Check if logo is already cached
    cached_path = get_cached_logo_path(domain)
    if cached_path:
        return cached_path
    
    # If download is disabled, return None
    if not download:
        return None
    
    # Try to get logo URL from API and download
    logo_url = get_logo_url_from_apistemic(domain)
    if logo_url:
        return download_and_cache_logo(logo_url, domain)
    
    return None


def main():
    if len(sys.argv) < 2:
        print("Usage: python get_stock_logo.py <website1> <website2> ... <websiteN>")
        sys.exit(1)

    websites = sys.argv[1:]
    result = {}

    for website in websites:
        try:
            logo_path = get_logo_path(website, download=True)
            
            if logo_path:
                result[website] = {"logo_path": logo_path}
            else:
                result[website] = {"error": f"Logo not found for website {website}"}
        
        except Exception as e:
            result[website] = {"error": str(e)}

    print(json.dumps(result))


if __name__ == "__main__":
    main()
